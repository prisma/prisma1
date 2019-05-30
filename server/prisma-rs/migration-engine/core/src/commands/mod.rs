mod apply_migration;
mod calculate_datamodel;
mod command;
mod infer_migration_steps;
mod list_migrations;
mod migration_progress;
mod unapply_migration;

pub use apply_migration::*;
pub use calculate_datamodel::*;
pub use command::*;
pub use infer_migration_steps::*;
pub use list_migrations::*;
pub use migration_progress::*;
pub use unapply_migration::*;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DataModelWarningOrError {
    #[serde(rename = "type")]
    pub tpe: String,
    pub field: Option<String>,
    pub message: String,
}
