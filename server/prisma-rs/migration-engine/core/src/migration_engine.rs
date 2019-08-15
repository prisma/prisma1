use crate::commands::CommandResult;
use crate::migration::datamodel_calculator::*;
use crate::migration::datamodel_migration_steps_inferrer::*;
use std::sync::Arc;
use datamodel::dml::*;
use migration_connector::*;

pub struct MigrationEngine<C, D>
where
    C: MigrationConnector<DatabaseMigration = D>,
    D: DatabaseMigrationMarker + 'static,
{
    datamodel_migration_steps_inferrer: Arc<dyn DataModelMigrationStepsInferrer>,
    datamodel_calculator: Arc<dyn DataModelCalculator>,
    connector: C,
}

impl<C, D> MigrationEngine<C, D>
where
    C: MigrationConnector<DatabaseMigration = D>,
    D: DatabaseMigrationMarker + 'static,
{
    pub fn new(connector: C) -> crate::Result<Self> {
        let engine = MigrationEngine {
            datamodel_migration_steps_inferrer: Arc::new(DataModelMigrationStepsInferrerImplWrapper {}),
            datamodel_calculator: Arc::new(DataModelCalculatorImpl {}),
            connector,
        };

        engine.init()?;

        Ok(engine)
    }

    pub fn init(&self) -> CommandResult<()> {
        self.connector().initialize()?;
        Ok(())
    }

    pub fn reset(&self) -> CommandResult<()> {
        self.connector().reset()?;
        Ok(())
    }

    pub fn connector(&self) -> &C {
        &self.connector
    }

    pub fn datamodel_migration_steps_inferrer(&self) -> &Arc<dyn DataModelMigrationStepsInferrer> {
        &self.datamodel_migration_steps_inferrer
    }

    pub fn datamodel_calculator(&self) -> &Arc<dyn DataModelCalculator> {
        &self.datamodel_calculator
    }

    pub fn render_datamodel(&self, datamodel: &Datamodel) -> String {
        datamodel::render(&datamodel).expect("Rendering the Datamodel failed.")
    }
}
