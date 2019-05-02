use crate::steps::*;
use prisma_datamodel::*;

pub trait DataModelMigrationStepsInferrer {
    fn infer(previous: &Schema, next: &Schema) -> Vec<MigrationStep>;
}

impl<'a> DataModelMigrationStepsInferrer for DataModelMigrationStepsInferrerImpl<'a> {
    fn infer(previous: &Schema, next: &Schema) -> Vec<MigrationStep> {
        let inferrer = DataModelMigrationStepsInferrerImpl { previous, next };
        inferrer.infer()
    }
}

struct DataModelMigrationStepsInferrerImpl<'a> {
    previous: &'a Schema,
    next: &'a Schema
}

impl<'a> DataModelMigrationStepsInferrerImpl<'a> {
    fn infer(&self) -> Vec<MigrationStep> {
        vec![]
    }
}