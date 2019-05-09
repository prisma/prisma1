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
        let models_to_create = self.models_to_create();
        let models_to_delete = self.models_to_delete();
        let fields_to_create = self.fields_to_create();
        let fields_to_delete = self.fields_to_delete();

        result.append(&mut Self::wrap_as_step(models_to_create, MigrationStep::CreateModel));
        result.append(&mut Self::wrap_as_step(models_to_delete, MigrationStep::DeleteModel));
        result.append(&mut Self::wrap_as_step(fields_to_create, MigrationStep::CreateField));
        result.append(&mut Self::wrap_as_step(fields_to_delete, MigrationStep::DeleteField));
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

    fn fields_to_delete(&self) -> Vec<DeleteField> {
        let mut result = Vec::new();
        for previous_model in self.previous.models() {
            for previous_field in previous_model.fields {
                let must_delete_field = match self.next.find_model(previous_model.name.clone()) {
                    None => true,
                    Some(next_model) => next_model.find_field(previous_field.name.clone()).is_none(),
                };
                if must_delete_field {
                    let step = DeleteField {
                        model: previous_model.name.clone(),
                        name: previous_field.name.clone(),                        
                    };
                    result.push(step);
                }
            }
        }
        result
    }

    fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<MigrationStep>
    where
        F: FnMut(T) -> MigrationStep,
    {
        steps.into_iter().map(|x| wrap_fn(x)).collect()
    }
}
