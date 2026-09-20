// preflight: no extra imports needed here
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PreflightCheck {
    pub source_total_bytes: u64,
    pub dest_available_bytes: u64,
    pub dest_path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PreflightResult {
    pub passes: bool,
    pub source_total_bytes: u64,
    pub dest_available_bytes: u64,
    pub required_bytes: u64,
    pub deficit_bytes: i64,
    pub message: String,
}

impl PreflightResult {
    pub fn source_formatted(&self) -> String { format_bytes(self.source_total_bytes) }
    pub fn available_formatted(&self) -> String { format_bytes(self.dest_available_bytes) }
}

impl PreflightCheck {
    pub fn new(source_total_bytes: u64, dest_path: &str) -> Self {
        Self {
            source_total_bytes,
            dest_available_bytes: get_available_space(dest_path),
            dest_path: dest_path.to_string(),
        }
    }

    pub fn run(&self) -> PreflightResult {
        // Add 5% overhead for filesystem metadata
        let required = (self.source_total_bytes as f64 * 1.05) as u64;
        let passes = self.dest_available_bytes >= required;
        let deficit = self.dest_available_bytes as i64 - required as i64;

        let message = if passes {
            format!(
                "Sufficient space: {} available, {} required.",
                format_bytes(self.dest_available_bytes),
                format_bytes(required)
            )
        } else {
            format!(
                "Insufficient space: {} available, {} required. Free up at least {}.",
                format_bytes(self.dest_available_bytes),
                format_bytes(required),
                format_bytes((-deficit) as u64)
            )
        };

        PreflightResult {
            passes,
            source_total_bytes: self.source_total_bytes,
            dest_available_bytes: self.dest_available_bytes,
            required_bytes: required,
            deficit_bytes: deficit,
            message,
        }
    }
}

#[cfg(target_os = "windows")]
fn get_available_space(path: &str) -> u64 {
    use std::os::windows::ffi::OsStrExt;
    use std::ffi::OsStr;

    extern "system" {
        fn GetDiskFreeSpaceExW(
            lpDirectoryName: *const u16,
            lpFreeBytesAvailableToCaller: *mut u64,
            lpTotalNumberOfBytes: *mut u64,
            lpTotalNumberOfFreeBytes: *mut u64,
        ) -> i32;
    }

    let wide: Vec<u16> = OsStr::new(path)
        .encode_wide()
        .chain(std::iter::once(0))
        .collect();

    let mut free: u64 = 0;
    let mut total: u64 = 0;
    let mut total_free: u64 = 0;

    unsafe {
        GetDiskFreeSpaceExW(wide.as_ptr(), &mut free, &mut total, &mut total_free);
    }
    free
}

#[cfg(not(target_os = "windows"))]
fn get_available_space(path: &str) -> u64 {
    // On Linux/Android/macOS use statvfs
    use std::mem::MaybeUninit;

    extern "C" {
        fn statvfs(path: *const libc::c_char, buf: *mut libc::statvfs) -> libc::c_int;
    }

    let c_path = std::ffi::CString::new(path).unwrap_or_default();
    let mut stat: MaybeUninit<libc::statvfs> = MaybeUninit::uninit();

    unsafe {
        if statvfs(c_path.as_ptr(), stat.as_mut_ptr()) == 0 {
            let s = stat.assume_init();
            s.f_bavail as u64 * s.f_bsize as u64
        } else {
            0
        }
    }
}

fn format_bytes(bytes: u64) -> String {
    const GB: u64 = 1_000_000_000;
    const MB: u64 = 1_000_000;
    const KB: u64 = 1_000;
    if bytes >= GB {
        format!("{:.1} GB", bytes as f64 / GB as f64)
    } else if bytes >= MB {
        format!("{:.1} MB", bytes as f64 / MB as f64)
    } else if bytes >= KB {
        format!("{:.1} KB", bytes as f64 / KB as f64)
    } else {
        format!("{} B", bytes)
    }
}
