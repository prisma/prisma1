use crate::steps::*;
use database_inspector::DatabaseSchema;
use prisma_models::Schema;
use std::sync::Arc;

pub trait MigrationStepsInferrer {
    fn infer(next: &Schema, database_schema: &DatabaseSchema) -> Vec<MigrationStep>;
}

pub struct MigrationStepsInferrerImpl<'a> {
    schema: &'a Schema,
    database_schema: &'a DatabaseSchema,
}

impl <'a> MigrationStepsInferrer for MigrationStepsInferrerImpl<'a> {
    fn infer(next: &Schema, database_schema: &DatabaseSchema) -> Vec<MigrationStep> {
        let inferer = MigrationStepsInferrerImpl{
            schema: next,
            database_schema: database_schema,
        };
        inferer.infer()
    }

}

impl <'a> MigrationStepsInferrerImpl<'a> {
    fn infer(&self) -> Vec<MigrationStep> {
        let mut result: Vec<MigrationStep> = vec!();
        let default = vec![];
        let next_models = self.schema.models.get().unwrap_or(&default);
        let mut create_model_steps: Vec<MigrationStep> = next_models
            .iter()
            .filter(|model| self.database_schema.table(model.db_name()).is_none())
            .map(|model| {
                let step = CreateModel {
                    name: model.name.clone(),
                    db_name: model.db_name_opt().map(|x| x.to_string()),
                    embedded: if model.is_embedded { Some(model.is_embedded) } else { None },
                };
                MigrationStep::CreateModel(step)
            })
            .collect();


        let mut create_field_steps: Vec<MigrationStep> = vec!();
        for model in next_models {
            for field in model.fields().scalar() {
                let step = CreateField {
                    model: model.name.clone(),
                    name: field.name.clone(),
                    tpe: field.type_identifier.userFriendlyTypeName(),
                    db_name: field.db_name_opt().map(|f| f.to_string()),
                    default: None,
                    id: field.id_behaviour_clone(),
                    is_created_at: if field.is_created_at() { Some(field.is_created_at()) } else { None },
                    is_updated_at: if field.is_updated_at() { Some(field.is_updated_at()) } else { None },
                    is_list: if field.is_list { Some(field.is_list) } else { None },
                    is_optional: if field.is_required { None } else { Some(true) },
                    scalar_list: field.scalar_list_behaviour_clone(),
                };
                create_field_steps.push(MigrationStep::CreateField(step))
            }
        }

        result.append(&mut create_model_steps);
        result.append(&mut create_field_steps);
        result
    }
}