use crate::migration::datamodel_calculator::*;
use crate::migration::datamodel_migration_steps_inferrer::*;
use datamodel::dml::*;
use datamodel::validator::Validator;
use migration_connector::*;

// todo: add MigrationConnector. does not work  because of GAT shinenigans

pub struct MigrationEngine<'a, T: DatabaseMigrationStepExt> {
    datamodel_migration_steps_inferrer: DataModelMigrationStepsInferrerImplWrapper,
    datamodel_calculator: DataModelCalculatorImpl,
    connector: &'a MigrationConnector<T>,
    schema_name: String,
}

impl<'a, T: DatabaseMigrationStepExt> std::panic::RefUnwindSafe for MigrationEngine<'a, T> {}

impl<'a, T: DatabaseMigrationStepExt> MigrationEngine<'a, T> {
    pub fn new(connector: &'a MigrationConnector<T>, schema_name: &str) -> MigrationEngine<'a, T> {
        let engine = MigrationEngine {
            datamodel_migration_steps_inferrer: DataModelMigrationStepsInferrerImplWrapper {},
            datamodel_calculator: DataModelCalculatorImpl {},
            connector: connector,
            schema_name: String::from(schema_name),
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

    pub fn schema_name(&self) -> &str {
        &self.schema_name
    }

    pub fn parse_datamodel(&self, datamodel_string: &String) -> Schema {
        let ast = datamodel::parser::parse(datamodel_string).unwrap();
        // TODO: this would need capabilities
        // TODO: Special directives are injected via EmptyAttachmentValidator.
        let validator = Validator::new();
        validator.validate(&ast).unwrap()
    }
}
