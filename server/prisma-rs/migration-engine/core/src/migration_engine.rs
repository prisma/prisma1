use crate::migration::datamodel_migration_steps_inferrer::*;
use datamodel::dml::*;
use datamodel::Validator;
use migration_connector::*;
use sql_migration_connector::SqlMigrationConnector;
use std::path::Path;
use std::sync::Arc;

// todo: add MigrationConnector. does not work  because of GAT shinenigans

pub struct MigrationEngine {
    datamodel_migration_steps_inferrer: Arc<DataModelMigrationStepsInferrer>,
}

impl MigrationEngine {
    pub fn new() -> Box<MigrationEngine> {
        let engine = MigrationEngine {
            datamodel_migration_steps_inferrer: Arc::new(DataModelMigrationStepsInferrerImplWrapper {}),
        };
        Box::new(engine)
    }

    pub fn datamodel_migration_steps_inferrer(&self) -> Arc<DataModelMigrationStepsInferrer> {
        Arc::clone(&self.datamodel_migration_steps_inferrer)
    }

    pub fn connector(&self) -> Arc<MigrationConnector<DatabaseMigrationStep = impl DatabaseMigrationStepExt>> {
        let file_path = file!(); // todo: the sqlite file name must be taken from the config
        let file_name = Path::new(file_path).file_stem().unwrap().to_str().unwrap();
        Arc::new(SqlMigrationConnector::new(file_name.to_string()))
    }

    pub fn parse_datamodel(&self, datamodel_string: &String) -> Schema {
        let ast = datamodel::parser::parse(datamodel_string);
        // TODO: this would need capabilities
        let validator = Validator::new();
        validator.validate(&ast)
    }
}
