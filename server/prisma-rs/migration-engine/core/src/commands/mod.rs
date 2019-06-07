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

use datamodel::dml::validator::directive::DirectiveValidator;
use datamodel::Source;
use migration_connector::{MigrationError, MigrationStep, MigrationWarning};
use std::collections::HashMap;

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

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DataSource {
    name: String,
    #[serde(rename(serialize = "type"))]
    tpe: String,
    url: String,
}
impl DataSource {
    fn as_dml_source(&self) -> Box<Source> {
        Box::new(self.clone())
    }
}

impl Source for DataSource {
    fn connector_type(&self) -> &str {
        &self.tpe
    }
    fn name(&self) -> &String {
        &self.name
    }
    fn url(&self) -> &String {
        &self.url
    }
    fn config(&self) -> HashMap<String, String> {
        HashMap::new()
    }
    fn get_field_directives(&self) -> Vec<Box<DirectiveValidator<datamodel::dml::Field>>> {
        Vec::new()
    }
    fn get_model_directives(&self) -> Vec<Box<DirectiveValidator<datamodel::dml::Model>>> {
        Vec::new()
    }
    fn get_enum_directives(&self) -> Vec<Box<DirectiveValidator<datamodel::dml::Enum>>> {
        Vec::new()
    }
}
