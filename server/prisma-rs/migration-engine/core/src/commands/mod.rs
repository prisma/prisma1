mod apply_migration;
mod calculate_database_steps;
mod calculate_datamodel;
mod command;
mod dmmf_to_dml;
mod infer_migration_steps;
mod list_datasources;
mod list_migrations;
mod migration_progress;
mod unapply_migration;

pub use apply_migration::*;
pub use calculate_database_steps::*;
pub use calculate_datamodel::*;
pub use command::*;
pub use dmmf_to_dml::*;
pub use infer_migration_steps::*;
pub use list_datasources::*;
pub use list_migrations::*;
pub use migration_progress::*;
pub use unapply_migration::*;

use migration_connector::{MigrationError, MigrationStep, MigrationWarning};

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DataModelWarningOrError {
    #[serde(rename = "type")]
    pub tpe: String,
    pub field: Option<String>,
    pub message: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MigrationStepsResultOutput {
    pub datamodel_steps: Vec<MigrationStep>,
    pub database_steps: serde_json::Value,
    pub warnings: Vec<MigrationWarning>,
    pub errors: Vec<MigrationError>,
    pub general_errors: Vec<String>,
}
