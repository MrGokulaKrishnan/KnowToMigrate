use std::path::Path;
use std::fs;
use serde::{Deserialize, Serialize};
use crate::scanner::{CategoryScanner, CategorySummary, FileCategory};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlannedFile {
    pub source_path: String,
    pub dest_relative_path: String,
    pub size_bytes: u64,
    pub category: FileCategory,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MigrationPlan {
    pub plan_id: String,
    pub source_platform: String,
    pub dest_platform: String,
    pub selected_categories: Vec<FileCategory>,
    pub files: Vec<PlannedFile>,
    pub total_files: u64,
    pub total_size_bytes: u64,
    pub estimated_seconds: u64,
    pub category_summaries: Vec<CategorySummary>,
}

impl MigrationPlan {
    pub fn total_size_formatted(&self) -> String {
        format_bytes(self.total_size_bytes)
    }

    pub fn estimated_time_formatted(&self) -> String {
        let mins = self.estimated_seconds / 60;
        let secs = self.estimated_seconds % 60;
        if mins > 60 {
            format!("{}h {}m", mins / 60, mins % 60)
        } else {
            format!("{}m {}s", mins, secs)
        }
    }
}

pub struct MigrationPlanBuilder {
    scanner: CategoryScanner,
    selected_categories: Vec<FileCategory>,
    source_platform: String,
    dest_platform: String,
    /// Estimated transfer speed in bytes/sec (default: 50 MB/s local WiFi)
    estimated_speed_bps: u64,
}

impl MigrationPlanBuilder {
    pub fn new() -> Self {
        Self {
            scanner: CategoryScanner::new_for_current_platform(),
            selected_categories: Vec::new(),
            source_platform: "Unknown".to_string(),
            dest_platform: "Unknown".to_string(),
            estimated_speed_bps: 50 * 1_000_000, // 50 MB/s
        }
    }

    pub fn source_platform(mut self, platform: &str) -> Self {
        self.source_platform = platform.to_string();
        self
    }

    pub fn dest_platform(mut self, platform: &str) -> Self {
        self.dest_platform = platform.to_string();
        self
    }

    pub fn select_categories(mut self, categories: Vec<FileCategory>) -> Self {
        self.selected_categories = categories;
        self
    }

    pub fn with_speed_estimate(mut self, bytes_per_sec: u64) -> Self {
        self.estimated_speed_bps = bytes_per_sec;
        self
    }

    pub fn build(self) -> MigrationPlan {
        let all_summaries = self.scanner.scan_all();
        let mut files: Vec<PlannedFile> = Vec::new();
        let mut total_size = 0u64;

        let filtered_summaries: Vec<CategorySummary> = all_summaries
            .into_iter()
            .filter(|s| self.selected_categories.contains(&s.category))
            .collect();

        for summary in &filtered_summaries {
            for path_str in &summary.sample_paths {
                let path = Path::new(path_str);
                if let Ok(meta) = fs::metadata(path) {
                    let size = meta.len();
                    total_size += size;
                    files.push(PlannedFile {
                        source_path: path_str.clone(),
                        dest_relative_path: path
                            .file_name()
                            .map(|n| format!("{}/{}", summary.category.display_name(), n.to_string_lossy()))
                            .unwrap_or_else(|| path_str.clone()),
                        size_bytes: size,
                        category: summary.category.clone(),
                    });
                }
            }
        }

        let estimated_seconds = if self.estimated_speed_bps > 0 {
            (total_size as f64 / self.estimated_speed_bps as f64) as u64
        } else {
            0
        };

        // Deduplicate by dest_relative_path
        files.sort_by(|a, b| a.dest_relative_path.cmp(&b.dest_relative_path));
        files.dedup_by(|a, b| a.dest_relative_path == b.dest_relative_path);

        MigrationPlan {
            plan_id: generate_plan_id(),
            source_platform: self.source_platform,
            dest_platform: self.dest_platform,
            selected_categories: self.selected_categories,
            total_files: files.len() as u64,
            total_size_bytes: total_size,
            estimated_seconds,
            files,
            category_summaries: filtered_summaries,
        }
    }
}

impl Default for MigrationPlanBuilder {
    fn default() -> Self {
        Self::new()
    }
}

fn generate_plan_id() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};
    let ts = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis();
    format!("plan-{:x}", ts)
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
