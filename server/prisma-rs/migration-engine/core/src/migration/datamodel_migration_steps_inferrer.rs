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

        let mut models_to_delete: Vec<MigrationStep> = self
            .models_to_delete()
            .into_iter()
            .map(|x| MigrationStep::DeleteModel(x))
            .collect();

        let mut fields_to_create: Vec<MigrationStep> = self
            .fields_to_create()
            .into_iter()
            .map(|x| MigrationStep::CreateField(x))
            .collect();

        result.append(&mut models_to_create);
        result.append(&mut models_to_delete);
        result.append(&mut fields_to_create);
        result
    }

    fn models_to_create(&self) -> Vec<CreateModel> {
        let mut result = Vec::new();
        for next_model in &self.next.models() {
            if !self.previous.has_model(next_model.name().to_string()) {
                let step = CreateModel {
                    name: next_model.name().to_string(),
                    db_name: next_model.database_name.as_ref().cloned(),
                    embedded: next_model.is_embedded,
                };
                result.push(step);
            }
        }

        result
    }

    fn models_to_delete(&self) -> Vec<DeleteModel> {
        let mut result = Vec::new();
        for previous_model in &self.previous.models() {
            if !self.next.has_model(previous_model.name.to_string()) {
                let step = DeleteModel {
                    name: previous_model.name().to_string(),
                };
                result.push(step);
            }
        }

        result
    }

    fn fields_to_create(&self) -> Vec<CreateField> {
        let mut result = Vec::new();
        for next_model in self.next.models() {
            for next_field in next_model.fields {
                let must_create_field = match self.previous.find_model(next_model.name.clone()) {
                    None => true,
                    Some(previous_model) => previous_model.find_field(next_field.name.clone()).is_none(),
                };
                if must_create_field {
                    let step = CreateField {
                        model: next_model.name.clone(),
                        name: next_field.name.clone(),
                        tpe: next_field.field_type,
                        arity: next_field.arity,
                        db_name: next_field.database_name.clone(),
                        default: next_field.default_value,
                        id: None, //field.id_behaviour_clone(),
                        is_created_at: None,
                        is_updated_at: None,
                        scalar_list: next_field.scalar_list_strategy,
                    };
                    result.push(step);
                }
            }
        }
        result
    }
}
