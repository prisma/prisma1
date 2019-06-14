use super::connector_loader::load_connector;
use crate::migration::datamodel_calculator::*;
use crate::migration::datamodel_migration_steps_inferrer::*;
use crate::commands::CommandResult;
use datamodel::dml::*;
use migration_connector::*;
use std::sync::Arc;

// todo: add MigrationConnector as a field. does not work  because of GAT shinenigans

pub struct MigrationEngine {
    config: String,
    underlying_database_must_exist: bool,
    datamodel_migration_steps_inferrer: Arc<DataModelMigrationStepsInferrer>,
    datamodel_calculator: Arc<DataModelCalculator>,
}

impl std::panic::RefUnwindSafe for MigrationEngine {}

impl MigrationEngine {
    pub fn new(config: &str, underlying_database_must_exist: bool) -> Box<MigrationEngine> {
        let engine = MigrationEngine {
            config: config.to_string(),
            underlying_database_must_exist: underlying_database_must_exist,
            datamodel_migration_steps_inferrer: Arc::new(DataModelMigrationStepsInferrerImplWrapper {}),
            datamodel_calculator: Arc::new(DataModelCalculatorImpl {}),
        };
        Box::new(engine)
    }

    pub fn init(&self) -> CommandResult<()> {
        self.connector().initialize()?;
        Ok(())        
    }

    pub fn reset(&self) -> CommandResult<()>  {
        self.connector().reset()?;
        Ok(())
    }

    pub fn datamodel_migration_steps_inferrer(&self) -> Arc<DataModelMigrationStepsInferrer> {
        Arc::clone(&self.datamodel_migration_steps_inferrer)
    }

    pub fn datamodel_calculator(&self) -> Arc<DataModelCalculator> {
        Arc::clone(&self.datamodel_calculator)
    }

    pub fn connector(&self) -> Arc<MigrationConnector<DatabaseMigration = impl DatabaseMigrationMarker>> {
        load_connector(&self.config, self.underlying_database_must_exist).expect("loading the connector failed.")
    }

    pub fn render_datamodel(&self, datamodel: &Datamodel) -> String {
        datamodel::render(&datamodel).expect("Rendering the Datamodel failed.")
    }
}
