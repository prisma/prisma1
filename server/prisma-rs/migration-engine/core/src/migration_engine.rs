use crate::migration::datamodel_calculator::*;
use crate::migration::datamodel_migration_steps_inferrer::*;
use datamodel::dml::*;
use migration_connector::*;
use sql_migration_connector::SqlMigrationConnector;
use std::sync::Arc;

// todo: add MigrationConnector as a field. does not work  because of GAT shinenigans

pub struct MigrationEngine {
    datamodel_migration_steps_inferrer: Arc<DataModelMigrationStepsInferrer>,
    datamodel_calculator: Arc<DataModelCalculator>,
}

impl std::panic::RefUnwindSafe for MigrationEngine {}

impl MigrationEngine {
    pub fn new() -> Box<MigrationEngine> {
        let engine = MigrationEngine {
            datamodel_migration_steps_inferrer: Arc::new(DataModelMigrationStepsInferrerImplWrapper {}),
            datamodel_calculator: Arc::new(DataModelCalculatorImpl {}),
        };
        Box::new(engine)
    }

    pub fn init(&self) {
        self.connector().initialize()
    }

    pub fn datamodel_migration_steps_inferrer(&self) -> Arc<DataModelMigrationStepsInferrer> {
        Arc::clone(&self.datamodel_migration_steps_inferrer)
    }

    pub fn datamodel_calculator(&self) -> Arc<DataModelCalculator> {
        Arc::clone(&self.datamodel_calculator)
    }

    pub fn connector(&self) -> Arc<MigrationConnector<DatabaseMigration = impl DatabaseMigrationMarker>> {
        Arc::new(SqlMigrationConnector::new(self.schema_name()))
    }

    pub fn schema_name(&self) -> String {
        // todo: the sqlite file name must be taken from the config
        "migration_engine".to_string()
    }

    pub fn render_datamodel(&self, datamodel: &Datamodel) -> String {
        datamodel::render(&datamodel).expect("Rendering the Datamodel failed.")
    }
}
