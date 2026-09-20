//! Path traversal guard.
//!
//! Validates that all file paths received from the remote peer resolve safely
//! inside the designated receive directory, rejecting:
//! - Absolute paths (e.g. `/etc/passwd`, `C:\Windows\...`).
//! - Paths containing `..` components.
//! - Paths containing null bytes (`\0`).
//! - Windows reserved device names (`CON`, `NUL`, `AUX`, `COM1`–`COM9`,
//!   `LPT1`–`LPT9`, `PRN`).
//! - Empty paths.
//! - Excessively long paths (> 4096 bytes).

use std::fmt;
use std::path::{Component, Path, PathBuf};

// ---------------------------------------------------------------------------
// Error type
// ---------------------------------------------------------------------------

/// Reason a path was rejected by [`PathGuard`].
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PathGuardError {
    /// The path is empty.
    EmptyPath,
    /// The path exceeds the maximum allowed length.
    PathTooLong(usize),
    /// The path contains a null byte.
    NullByte,
    /// The path is absolute (`/foo` or `C:\foo`).
    AbsolutePath,
    /// The path contains a `..` component.
    ParentDirectoryTraversal,
    /// One of the path components is a Windows reserved device name.
    ReservedName(String),
    /// After canonicalisation the path escapes the receive directory.
    EscapesReceiveDir,
}

impl fmt::Display for PathGuardError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::EmptyPath => write!(f, "Path is empty"),
            Self::PathTooLong(n) => write!(f, "Path is {n} bytes, exceeds 4096-byte limit"),
            Self::NullByte => write!(f, "Path contains a null byte"),
            Self::AbsolutePath => write!(f, "Absolute paths are not allowed"),
            Self::ParentDirectoryTraversal => {
                write!(f, "Path contains '..' (parent directory traversal)")
            }
            Self::ReservedName(name) => {
                write!(f, "'{name}' is a reserved device name on Windows")
            }
            Self::EscapesReceiveDir => {
                write!(f, "Resolved path escapes the designated receive directory")
            }
        }
    }
}

impl std::error::Error for PathGuardError {}

// ---------------------------------------------------------------------------
// Reserved Windows device names
// ---------------------------------------------------------------------------

fn is_reserved_name(s: &str) -> bool {
    let upper = s.to_uppercase();
    // Remove any extension (e.g. `NUL.txt` → `NUL`)
    let base = upper.split('.').next().unwrap_or(&upper);
    matches!(
        base,
        "CON" | "PRN" | "AUX" | "NUL"
            | "COM1" | "COM2" | "COM3" | "COM4" | "COM5" | "COM6" | "COM7" | "COM8" | "COM9"
            | "LPT1" | "LPT2" | "LPT3" | "LPT4" | "LPT5" | "LPT6" | "LPT7" | "LPT8" | "LPT9"
    )
}

// ---------------------------------------------------------------------------
// PathGuard
// ---------------------------------------------------------------------------

/// Guards a receive directory against path traversal and other attacks.
pub struct PathGuard {
    /// The absolute, canonicalised receive directory.
    receive_dir: PathBuf,
}

impl PathGuard {
    /// Create a [`PathGuard`] rooted at `receive_dir`.
    ///
    /// The directory is **not** required to already exist on disk; validation
    /// relies on lexical analysis plus a prefix check on the resolved path.
    pub fn new(receive_dir: impl Into<PathBuf>) -> Self {
        Self {
            receive_dir: receive_dir.into(),
        }
    }

    /// Validate `relative_path` and return the absolute destination path if safe.
    ///
    /// `relative_path` must be a POSIX-style relative path (forward slashes, no
    /// leading `/`).  Windows backslashes are also accepted for compatibility.
    pub fn validate(&self, relative_path: &str) -> Result<PathBuf, PathGuardError> {
        // 1. Empty check.
        if relative_path.is_empty() {
            return Err(PathGuardError::EmptyPath);
        }

        // 2. Length check.
        if relative_path.len() > 4096 {
            return Err(PathGuardError::PathTooLong(relative_path.len()));
        }

        // 3. Null byte check.
        if relative_path.contains('\0') {
            return Err(PathGuardError::NullByte);
        }

        // Normalise backslashes to forward slashes for cross-platform parsing.
        let normalised = relative_path.replace('\\', "/");
        let path = Path::new(&normalised);

        // 4. Absolute path check.
        if path.is_absolute() {
            return Err(PathGuardError::AbsolutePath);
        }

        // 5. Component-level checks.
        for component in path.components() {
            match component {
                Component::ParentDir => {
                    return Err(PathGuardError::ParentDirectoryTraversal);
                }
                Component::RootDir | Component::Prefix(_) => {
                    return Err(PathGuardError::AbsolutePath);
                }
                Component::Normal(os_str) => {
                    let s = os_str.to_string_lossy();
                    if is_reserved_name(&s) {
                        return Err(PathGuardError::ReservedName(s.into_owned()));
                    }
                }
                Component::CurDir => {} // `.` is harmless
            }
        }

        // 6. Construct candidate absolute path.
        let candidate = self.receive_dir.join(path);

        // 7. Prefix check — ensure candidate starts with receive_dir.
        //    We cannot use `canonicalize` because the file doesn't exist yet;
        //    instead we resolve the path lexically.
        let resolved = lexical_clean(&candidate);
        let base = lexical_clean(&self.receive_dir);

        if !resolved.starts_with(&base) {
            return Err(PathGuardError::EscapesReceiveDir);
        }

        Ok(resolved)
    }
}

/// Lexically resolve `.` and `..` components without requiring the path to exist.
fn lexical_clean(path: &Path) -> PathBuf {
    let mut stack: Vec<std::ffi::OsString> = Vec::new();
    for component in path.components() {
        match component {
            Component::ParentDir => {
                stack.pop();
            }
            Component::CurDir => {}
            other => {
                stack.push(other.as_os_str().to_os_string());
            }
        }
    }
    stack.iter().collect()
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    fn guard() -> PathGuard {
        PathGuard::new("/receive")
    }

    #[test]
    fn simple_relative_path_ok() {
        let g = guard();
        let p = g.validate("photos/img.jpg").unwrap();
        assert_eq!(p, PathBuf::from("/receive/photos/img.jpg"));
    }

    #[test]
    fn dotdot_rejected() {
        let g = guard();
        assert_eq!(
            g.validate("../etc/passwd"),
            Err(PathGuardError::ParentDirectoryTraversal)
        );
    }

    #[test]
    fn absolute_path_rejected() {
        let g = guard();
        assert_eq!(g.validate("/etc/passwd"), Err(PathGuardError::AbsolutePath));
    }

    #[test]
    fn null_byte_rejected() {
        let g = guard();
        assert_eq!(g.validate("foo\0bar"), Err(PathGuardError::NullByte));
    }

    #[test]
    fn empty_path_rejected() {
        let g = guard();
        assert_eq!(g.validate(""), Err(PathGuardError::EmptyPath));
    }

    #[test]
    fn reserved_names_rejected() {
        let g = guard();
        for name in &["CON", "NUL", "COM1", "LPT9", "PRN", "AUX"] {
            assert!(
                matches!(g.validate(name), Err(PathGuardError::ReservedName(_))),
                "Expected ReservedName for {name}"
            );
        }
    }

    #[test]
    fn reserved_name_with_extension_rejected() {
        let g = guard();
        assert!(matches!(
            g.validate("NUL.txt"),
            Err(PathGuardError::ReservedName(_))
        ));
    }

    #[test]
    fn backslash_path_normalised_and_validated() {
        let g = guard();
        let p = g.validate("docs\\report.pdf").unwrap();
        assert_eq!(p, PathBuf::from("/receive/docs/report.pdf"));
    }
}
