use crate::steps::*;
use database_inspector::DatabaseSchema;
use prisma_models::Schema;

pub trait MigrationStepsInferrer {
    fn infer(next: &Schema, database_schema: &DatabaseSchema) -> Vec<MigrationStep>;
}

pub struct MigrationStepsInferrerImpl;

impl MigrationStepsInferrer for MigrationStepsInferrerImpl {
    fn infer(next: &Schema, database_schema: &DatabaseSchema) -> Vec<MigrationStep> {
        let default = vec![];
        let next_models = next.models.get().unwrap_or(&default);
        next_models
            .iter()
            .filter(|model| database_schema.table(model.db_name()).is_none())
            .map(|model| {
                let step = CreateModel {
                    name: model.name.clone(),
                    db_name: model.db_name_opt().map(|x| x.to_string()),
                    embedded: Some(model.is_embedded),
                };
                MigrationStep::CreateModel(step)
            })
            .collect()
    }
}
