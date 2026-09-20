pub mod scanner;
pub mod plan;
pub mod preflight;

pub use scanner::{CategoryScanner, CategorySummary, FileCategory};
pub use plan::{MigrationPlan, MigrationPlanBuilder, PlannedFile};
pub use preflight::{PreflightCheck, PreflightResult};
