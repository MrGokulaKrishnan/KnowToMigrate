use std::path::{Path, PathBuf};
use std::fs;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub enum FileCategory {
    Photos,
    Videos,
    Music,
    Documents,
    Downloads,
    Desktop,
    WhatsApp,
    AppData,
    Other,
}

impl FileCategory {
    pub fn display_name(&self) -> &'static str {
        match self {
            Self::Photos => "Photos",
            Self::Videos => "Videos",
            Self::Music => "Music",
            Self::Documents => "Documents",
            Self::Downloads => "Downloads",
            Self::Desktop => "Desktop",
            Self::WhatsApp => "WhatsApp",
            Self::AppData => "App Data",
            Self::Other => "Other",
        }
    }

    pub fn icon(&self) -> &'static str {
        match self {
            Self::Photos => "🖼️",
            Self::Videos => "🎬",
            Self::Music => "🎵",
            Self::Documents => "📄",
            Self::Downloads => "⬇️",
            Self::Desktop => "🖥️",
            Self::WhatsApp => "💬",
            Self::AppData => "⚙️",
            Self::Other => "📁",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CategorySummary {
    pub category: FileCategory,
    pub file_count: u64,
    pub total_size_bytes: u64,
    pub sample_paths: Vec<String>,
}

impl CategorySummary {
    pub fn total_size_formatted(&self) -> String {
        format_bytes(self.total_size_bytes)
    }
}

pub struct CategoryScanner {
    platform: Platform,
}

#[derive(Debug, Clone)]
pub enum Platform {
    Windows,
    Android,
    MacOs,
    Linux,
}

impl CategoryScanner {
    pub fn new_for_current_platform() -> Self {
        #[cfg(target_os = "windows")]
        let platform = Platform::Windows;
        #[cfg(target_os = "linux")]
        let platform = Platform::Linux;
        #[cfg(target_os = "macos")]
        let platform = Platform::MacOs;
        #[cfg(not(any(target_os = "windows", target_os = "linux", target_os = "macos")))]
        let platform = Platform::Android;

        Self { platform }
    }

    pub fn with_platform(platform: Platform) -> Self {
        Self { platform }
    }

    pub fn scan_all(&self) -> Vec<CategorySummary> {
        let category_paths = self.get_category_paths();
        let mut results = Vec::new();

        for (category, paths) in category_paths {
            let mut file_count = 0u64;
            let mut total_size = 0u64;
            let mut sample_paths = Vec::new();

            for base_path in &paths {
                let path = Path::new(base_path);
                if !path.exists() {
                    continue;
                }
                if let Ok(entries) = self.scan_dir(path) {
                    for entry in &entries {
                        file_count += 1;
                        total_size += entry.size;
                        if sample_paths.len() < 3 {
                            sample_paths.push(entry.path.to_string_lossy().to_string());
                        }
                    }
                }
            }

            if file_count > 0 {
                results.push(CategorySummary {
                    category,
                    file_count,
                    total_size_bytes: total_size,
                    sample_paths,
                });
            }
        }

        results
    }

    fn get_category_paths(&self) -> Vec<(FileCategory, Vec<String>)> {
        match self.platform {
            Platform::Windows => {
                let user = std::env::var("USERPROFILE").unwrap_or_else(|_| "C:\\Users\\User".to_string());
                vec![
                    (FileCategory::Photos, vec![
                        format!("{}\\Pictures", user),
                        format!("{}\\OneDrive\\Pictures", user),
                    ]),
                    (FileCategory::Videos, vec![
                        format!("{}\\Videos", user),
                        format!("{}\\OneDrive\\Videos", user),
                    ]),
                    (FileCategory::Music, vec![
                        format!("{}\\Music", user),
                    ]),
                    (FileCategory::Documents, vec![
                        format!("{}\\Documents", user),
                        format!("{}\\OneDrive\\Documents", user),
                    ]),
                    (FileCategory::Downloads, vec![
                        format!("{}\\Downloads", user),
                    ]),
                    (FileCategory::Desktop, vec![
                        format!("{}\\Desktop", user),
                    ]),
                ]
            }
            Platform::Android => {
                vec![
                    (FileCategory::Photos, vec![
                        "/sdcard/DCIM".to_string(),
                        "/sdcard/Pictures".to_string(),
                    ]),
                    (FileCategory::Videos, vec![
                        "/sdcard/DCIM".to_string(),
                        "/sdcard/Movies".to_string(),
                    ]),
                    (FileCategory::Music, vec![
                        "/sdcard/Music".to_string(),
                    ]),
                    (FileCategory::Documents, vec![
                        "/sdcard/Documents".to_string(),
                    ]),
                    (FileCategory::Downloads, vec![
                        "/sdcard/Download".to_string(),
                    ]),
                    (FileCategory::WhatsApp, vec![
                        "/sdcard/WhatsApp".to_string(),
                        "/sdcard/Android/media/com.whatsapp/WhatsApp".to_string(),
                    ]),
                ]
            }
            Platform::MacOs => {
                let home = std::env::var("HOME").unwrap_or_else(|_| "/Users/user".to_string());
                vec![
                    (FileCategory::Photos, vec![format!("{}/Pictures", home)]),
                    (FileCategory::Videos, vec![format!("{}/Movies", home)]),
                    (FileCategory::Music, vec![format!("{}/Music", home)]),
                    (FileCategory::Documents, vec![format!("{}/Documents", home)]),
                    (FileCategory::Downloads, vec![format!("{}/Downloads", home)]),
                    (FileCategory::Desktop, vec![format!("{}/Desktop", home)]),
                ]
            }
            Platform::Linux => {
                let home = std::env::var("HOME").unwrap_or_else(|_| "/home/user".to_string());
                vec![
                    (FileCategory::Photos, vec![format!("{}/Pictures", home)]),
                    (FileCategory::Videos, vec![format!("{}/Videos", home)]),
                    (FileCategory::Music, vec![format!("{}/Music", home)]),
                    (FileCategory::Documents, vec![format!("{}/Documents", home)]),
                    (FileCategory::Downloads, vec![format!("{}/Downloads", home)]),
                    (FileCategory::Desktop, vec![format!("{}/Desktop", home)]),
                ]
            }
        }
    }

    fn scan_dir(&self, dir: &Path) -> std::io::Result<Vec<FileEntry>> {
        let mut entries = Vec::new();
        if let Ok(read_dir) = fs::read_dir(dir) {
            for entry in read_dir.flatten() {
                let path = entry.path();
                let meta = match entry.metadata() {
                    Ok(m) => m,
                    Err(_) => continue,
                };
                if meta.is_file() {
                    entries.push(FileEntry { path, size: meta.len() });
                } else if meta.is_dir() {
                    // Recurse one level
                    if let Ok(sub) = self.scan_dir(&path) {
                        entries.extend(sub);
                    }
                }
            }
        }
        Ok(entries)
    }
}

struct FileEntry {
    path: PathBuf,
    size: u64,
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
