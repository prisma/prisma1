use crate::steps::MigrationStep;
use database_inspector::DatabaseSchema;
use prisma_models::Schema;

pub trait MigrationStepsInferrer {
    fn infer(&self, previous: Schema, database_schema: DatabaseSchema) -> Vec<MigrationStep>;
}

pub struct MigrationStepsInferrerImpl;

impl MigrationStepsInferrer for MigrationStepsInferrerImpl {
    fn infer(&self, previous: Schema, database_schema: DatabaseSchema) -> Vec<MigrationStep> {
        unimplemented!()
    }
}
