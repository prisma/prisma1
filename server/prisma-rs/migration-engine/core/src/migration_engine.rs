use crate::migration::datamodel_calculator::*;
use crate::migration::datamodel_migration_steps_inferrer::*;
use datamodel::dml::*;
use datamodel::validator::Validator;
use migration_connector::*;
use sql_migration_connector::SqlMigrationConnector;
use std::path::Path;
use std::sync::Arc;

// todo: add MigrationConnector. does not work  because of GAT shinenigans

pub struct MigrationEngine<'a, T: DatabaseMigrationStepExt> {
    datamodel_migration_steps_inferrer: DataModelMigrationStepsInferrerImplWrapper,
    datamodel_calculator: DataModelCalculatorImpl,
    connector: &'a MigrationConnector<T>
}

impl<'a, T: DatabaseMigrationStepExt> std::panic::RefUnwindSafe for MigrationEngine<'a, T> {}

impl<'a, T: DatabaseMigrationStepExt> MigrationEngine<'a, T> {
    pub fn new(connector: &'a MigrationConnector<T>) -> MigrationEngine<'a, T> {
        let engine = MigrationEngine {
            datamodel_migration_steps_inferrer: DataModelMigrationStepsInferrerImplWrapper {},
            datamodel_calculator: DataModelCalculatorImpl {},
            connector: connector
        };
        engine.connector().initialize().unwrap();
        engine
    }

    pub fn datamodel_migration_steps_inferrer(&self) -> &DataModelMigrationStepsInferrer {
        &self.datamodel_migration_steps_inferrer
    }

    pub fn datamodel_calculator(&self) -> &DataModelCalculator {
        &self.datamodel_calculator
    }

    pub fn connector(&self) -> &'a MigrationConnector<T> {
        self.connector
    }

    pub fn schema_name(&self) -> String {
        let file_path = file!(); // todo: the sqlite file name must be taken from the config
        let file_name = Path::new(file_path).file_stem().unwrap().to_str().unwrap();
        file_name.to_string()
    }

    pub fn parse_datamodel(&self, datamodel_string: &String) -> Schema {
        let ast = datamodel::parser::parse(datamodel_string).unwrap();
        // TODO: this would need capabilities
        // TODO: Special directives are injected via EmptyAttachmentValidator.
        let validator = Validator::new();
        validator.validate(&ast).unwrap()
    }
}
