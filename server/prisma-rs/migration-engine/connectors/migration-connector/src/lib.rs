use prisma_datamodel::Schema;

trait MigrationConnector {
    type DatabaseMigrationStep;

    fn infer_database_steps(&self, previous: &Schema, current: &Schema) -> Vec<Self::DatabaseMigrationStep>;
}
