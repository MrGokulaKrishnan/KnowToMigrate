//! Transfer session finite state machine.
//!
//! Models the lifecycle of a KTM file transfer on both sides.  All state
//! transitions are validated; illegal transitions return an error rather than
//! panicking.

use std::fmt;
use std::time::Instant;

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

/// All possible states a transfer session can be in.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum TransferState {
    /// Not yet started.
    Idle,
    /// Performing ECDH key exchange + device authentication.
    Pairing,
    /// Exchanging manifest and performing pre-transfer storage check.
    Preflight,
    /// Actively streaming chunk data.
    Streaming,
    /// Temporarily paused by user or network.
    Paused,
    /// Sender done; receiver is verifying file integrity.
    Verifying,
    /// Transfer completed successfully.
    Complete,
    /// An unrecoverable error occurred.
    Failed,
    /// Transfer was explicitly cancelled by either party.
    Cancelled,
}

impl fmt::Display for TransferState {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{self:?}")
    }
}

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

/// Events that drive state transitions.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TransferEvent {
    PairStart,
    PairSuccess,
    PairFailed,
    PreflightStart,
    PreflightOk,
    PreflightFailed,
    StreamStart,
    Pause,
    Resume,
    StreamDone,
    VerifyOk,
    VerifyFailed,
    Cancel,
    Error,
}

// ---------------------------------------------------------------------------
// Errors
// ---------------------------------------------------------------------------

/// Error returned for invalid state transitions.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct TransitionError {
    pub from: TransferState,
    pub event: TransferEvent,
}

impl fmt::Display for TransitionError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "Invalid transition: {:?} --{:?}--> (no valid target)",
            self.from, self.event
        )
    }
}

impl std::error::Error for TransitionError {}

// ---------------------------------------------------------------------------
// TransferSession FSM
// ---------------------------------------------------------------------------

/// A transfer session with full lifecycle tracking.
pub struct TransferSession {
    /// Stable transfer UUID.
    pub transfer_id: String,
    /// Current FSM state.
    pub state: TransferState,
    /// When this session was created.
    pub created_at: Instant,
    /// When the current state was entered.
    pub state_entered_at: Instant,
    /// Bytes transferred so far.
    pub bytes_transferred: u64,
    /// Total bytes to transfer (0 if unknown).
    pub total_bytes: u64,
    /// Error message, populated when state is [`TransferState::Failed`].
    pub error: Option<String>,
}

impl TransferSession {
    /// Create a new session in the [`TransferState::Idle`] state.
    pub fn new(transfer_id: impl Into<String>, total_bytes: u64) -> Self {
        let now = Instant::now();
        Self {
            transfer_id: transfer_id.into(),
            state: TransferState::Idle,
            created_at: now,
            state_entered_at: now,
            bytes_transferred: 0,
            total_bytes,
            error: None,
        }
    }

    /// Apply `event` and transition to the next state.
    ///
    /// Returns `Ok(new_state)` on success or `Err(TransitionError)` on an
    /// invalid transition.
    pub fn apply(&mut self, event: TransferEvent) -> Result<TransferState, TransitionError> {
        let next = self.next_state(event)?;
        self.state = next;
        self.state_entered_at = Instant::now();
        Ok(next)
    }

    /// Apply an event, also recording an error message (used with
    /// [`TransferEvent::Error`] / [`TransferEvent::PairFailed`] etc.).
    pub fn apply_with_error(
        &mut self,
        event: TransferEvent,
        error: impl Into<String>,
    ) -> Result<TransferState, TransitionError> {
        let next = self.apply(event)?;
        self.error = Some(error.into());
        Ok(next)
    }

    /// Update byte progress (call after each chunk ACK).
    pub fn add_bytes(&mut self, n: u64) {
        self.bytes_transferred += n;
    }

    /// Progress as a fraction 0.0–1.0, or `None` if total is unknown.
    pub fn progress(&self) -> Option<f64> {
        if self.total_bytes == 0 {
            return None;
        }
        Some(self.bytes_transferred as f64 / self.total_bytes as f64)
    }

    /// Seconds spent in the current state.
    pub fn time_in_state(&self) -> f64 {
        self.state_entered_at.elapsed().as_secs_f64()
    }

    /// Is the session in a terminal state?
    pub fn is_terminal(&self) -> bool {
        matches!(
            self.state,
            TransferState::Complete | TransferState::Failed | TransferState::Cancelled
        )
    }

    // -----------------------------------------------------------------------
    // Transition table
    // -----------------------------------------------------------------------

    fn next_state(
        &self,
        event: TransferEvent,
    ) -> Result<TransferState, TransitionError> {
        use TransferEvent::*;
        use TransferState::*;

        let next = match (self.state, event) {
            // From Idle
            (Idle, PairStart)       => Pairing,
            (Idle, Cancel)          => Cancelled,

            // From Pairing
            (Pairing, PairSuccess)   => Preflight,
            (Pairing, PairFailed)    => Failed,
            (Pairing, Cancel)        => Cancelled,
            (Pairing, Error)         => Failed,

            // From Preflight
            (Preflight, PreflightOk)     => Streaming,
            (Preflight, PreflightFailed) => Failed,
            (Preflight, Cancel)          => Cancelled,
            (Preflight, Error)           => Failed,

            // From Streaming
            (Streaming, StreamDone) => Verifying,
            (Streaming, Pause)      => Paused,
            (Streaming, Cancel)     => Cancelled,
            (Streaming, Error)      => Failed,

            // From Paused
            (Paused, Resume)  => Streaming,
            (Paused, Cancel)  => Cancelled,
            (Paused, Error)   => Failed,

            // From Verifying
            (Verifying, VerifyOk)     => Complete,
            (Verifying, VerifyFailed) => Failed,
            (Verifying, Cancel)       => Cancelled,
            (Verifying, Error)        => Failed,

            // Terminal states are absorbing.
            (Complete | Failed | Cancelled, _) => {
                return Err(TransitionError { from: self.state, event });
            }

            // Any other combination is invalid.
            _ => return Err(TransitionError { from: self.state, event }),
        };

        Ok(next)
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    fn happy_path_session() -> TransferSession {
        let mut s = TransferSession::new("tid-1", 1024);
        s.apply(TransferEvent::PairStart).unwrap();
        s.apply(TransferEvent::PairSuccess).unwrap();
        s.apply(TransferEvent::PreflightOk).unwrap();
        s.apply(TransferEvent::StreamDone).unwrap();
        s.apply(TransferEvent::VerifyOk).unwrap();
        s
    }

    #[test]
    fn happy_path_reaches_complete() {
        let s = happy_path_session();
        assert_eq!(s.state, TransferState::Complete);
    }

    #[test]
    fn cancel_from_streaming() {
        let mut s = TransferSession::new("tid-2", 0);
        s.apply(TransferEvent::PairStart).unwrap();
        s.apply(TransferEvent::PairSuccess).unwrap();
        s.apply(TransferEvent::PreflightOk).unwrap();
        s.apply(TransferEvent::Cancel).unwrap();
        assert_eq!(s.state, TransferState::Cancelled);
    }

    #[test]
    fn pause_resume() {
        let mut s = TransferSession::new("tid-3", 0);
        s.apply(TransferEvent::PairStart).unwrap();
        s.apply(TransferEvent::PairSuccess).unwrap();
        s.apply(TransferEvent::PreflightOk).unwrap();
        s.apply(TransferEvent::Pause).unwrap();
        assert_eq!(s.state, TransferState::Paused);
        s.apply(TransferEvent::Resume).unwrap();
        assert_eq!(s.state, TransferState::Streaming);
    }

    #[test]
    fn invalid_transition_returns_error() {
        let mut s = TransferSession::new("tid-4", 0);
        // Cannot go from Idle directly to StreamDone.
        assert!(s.apply(TransferEvent::StreamDone).is_err());
    }

    #[test]
    fn terminal_state_is_absorbing() {
        let s = happy_path_session();
        assert!(s.is_terminal());
    }

    #[test]
    fn progress_fraction() {
        let mut s = TransferSession::new("tid-5", 100);
        s.add_bytes(50);
        assert!((s.progress().unwrap() - 0.5).abs() < f64::EPSILON);
    }
}
