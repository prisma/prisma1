use crate::migration::datamodel_calculator::*;
use crate::migration::datamodel_migration_steps_inferrer::*;
use datamodel::dml::*;
use migration_connector::*;
use std::sync::Arc;
use super::connector_loader::load_connector;

// todo: add MigrationConnector as a field. does not work  because of GAT shinenigans

pub struct MigrationEngine {
    config: String,
    datamodel_migration_steps_inferrer: Arc<DataModelMigrationStepsInferrer>,
    datamodel_calculator: Arc<DataModelCalculator>,
}

impl std::panic::RefUnwindSafe for MigrationEngine {}

impl MigrationEngine {
    pub fn new(config: &str) -> Box<MigrationEngine> {
        let engine = MigrationEngine {
            config: config.to_string(),
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
        load_connector(&self.config).expect("loading the connector failed.")
    }

    pub fn render_datamodel(&self, datamodel: &Datamodel) -> String {
        datamodel::render(&datamodel).expect("Rendering the Datamodel failed.")
    }
}
