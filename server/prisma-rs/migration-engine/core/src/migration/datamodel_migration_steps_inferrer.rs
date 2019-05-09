use datamodel::*;
use migration_connector::steps::*;

pub trait DataModelMigrationStepsInferrer {
    fn infer(previous: Schema, next: Schema) -> Vec<MigrationStep>;
}

impl DataModelMigrationStepsInferrer for DataModelMigrationStepsInferrerImpl {
    fn infer(previous: Schema, next: Schema) -> Vec<MigrationStep> {
        let inferrer = DataModelMigrationStepsInferrerImpl { previous, next };
        inferrer.infer_internal()
    }
}

#[allow(dead_code)]
pub struct DataModelMigrationStepsInferrerImpl {
    previous: Schema,
    next: Schema,
}

impl DataModelMigrationStepsInferrerImpl {
    fn infer_internal(&self) -> Vec<MigrationStep> {
        let mut result: Vec<MigrationStep> = Vec::new();
        let mut models_to_create: Vec<MigrationStep> = self
            .models_to_create()
            .into_iter()
            .map(|x| MigrationStep::CreateModel(x))
            .collect();
        let mut fields_to_create: Vec<MigrationStep> = self
            .fields_to_create()
            .into_iter()
            .map(|x| MigrationStep::CreateField(x))
            .collect();

        result.append(&mut models_to_create);
        result.append(&mut fields_to_create);
        result
    }

    fn models_to_create(&self) -> Vec<CreateModel> {
        let mut result = Vec::new();
        for next_model in &self.next.models {
            match next_model {
                ModelOrEnum::Model(ref model) => {
                    if !self.previous.has_model(model.name().to_string()) {
                        let step = CreateModel {
                            name: model.name().to_string(),
                            db_name: model.database_name.as_ref().cloned(),
                            embedded: model.is_embedded,
                        };
                        result.push(step);
                    }
                }
                _ => {}
            }
        }

        result
    }

    fn fields_to_create(&self) -> Vec<CreateField> {
        let mut result = Vec::new();
        for next_model in self.next.models() {
            if let Some(previous_model) = self.previous.find_model(next_model.name.clone()) {
                for next_field in next_model.fields {
                    // if let None = previous_model.find_field(next_field.name.clone()) {
                    //     let step = CreateField {
                    //         model: next_model.name.clone(),
                    //         name: next_field.name.clone(),
                    //         tpe: "String".to_string(),
                    //         db_name: next_field.database_name.clone(),
                    //         default: None,
                    //         id: None, //field.id_behaviour_clone(),
                    //         is_created_at: Some(false),
                    //         is_updated_at: Some(false),
                    //         is_list: Some(false),
                    //         is_optional: Some(false),
                    //         scalar_list: None, //field.scalar_list_behaviour_clone(),
                    //     };
                    //     result.push(step);
                    // }
                }
            }
        }
        result
    }
}
