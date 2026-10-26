/**
 * KnowToMigrate - Finite State Machine for Transfer Sessions
 */

import { EventEmitter } from 'node:events';
import { SessionState, TransferProgress, ConnectionTransport } from '@knowtomigrate/protocol-types';

export interface StateMachineEvents {
  'stateChanged': (newState: SessionState, oldState: SessionState) => void;
  'progress': (progress: TransferProgress) => void;
  'error': (err: Error) => void;
}

export class TransferStateMachine extends EventEmitter {
  private currentState: SessionState = 'idle';
  private sessionId: string;
  private transport: ConnectionTransport = 'direct_lan';

  // Allowed state transition matrix
  private static readonly VALID_TRANSITIONS: Record<SessionState, SessionState[]> = {
    idle: ['preparing', 'connecting', 'failed'],
    preparing: ['connecting', 'idle', 'failed', 'cancelled'],
    connecting: ['authenticating', 'preflight', 'failed', 'cancelled'],
    authenticating: ['preflight', 'failed', 'cancelled'],
    preflight: ['streaming', 'paused', 'failed', 'cancelled'],
    streaming: ['paused', 'verifying', 'failed', 'cancelled'],
    paused: ['streaming', 'failed', 'cancelled'],
    verifying: ['completed', 'failed', 'cancelled'],
    completed: ['idle'],
    failed: ['idle', 'preparing'],
    cancelled: ['idle']
  };

  constructor(sessionId: string) {
    super();
    this.sessionId = sessionId;
  }

  public getState(): SessionState {
    return this.currentState;
  }

  public getSessionId(): string {
    return this.sessionId;
  }

  public setTransport(transport: ConnectionTransport): void {
    this.transport = transport;
  }

  public getTransport(): ConnectionTransport {
    return this.transport;
  }

  public transition(newState: SessionState): void {
    if (this.currentState === newState) return;

    const allowed = TransferStateMachine.VALID_TRANSITIONS[this.currentState];
    if (!allowed || !allowed.includes(newState)) {
      throw new Error(`Invalid state transition: cannot transition from "${this.currentState}" to "${newState}"`);
    }

    const oldState = this.currentState;
    this.currentState = newState;
    this.emit('stateChanged', newState, oldState);
  }

  public updateProgress(progress: TransferProgress): void {
    progress.state = this.currentState;
    progress.transport = this.transport;
    this.emit('progress', progress);
  }
}
