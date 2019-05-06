pub mod apply_next_migration_step;
pub mod command;
pub mod start_migration;
pub mod suggest_migration_step;
pub mod infer_migration_steps;
pub mod list_migrations;
pub mod migration_progress;
pub mod apply_migration;
pub mod unapply_migration;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DataModelWarningOrError {
    #[serde(rename = "type")]
    pub tpe: String,
    pub field: Option<String>,
    pub message: String,
}
