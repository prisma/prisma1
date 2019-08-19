use datamodel::*;
use migration_connector::steps::*;

pub trait DataModelMigrationStepsInferrer: Send + Sync + 'static {
    fn infer(&self, previous: &Datamodel, next: &Datamodel) -> Vec<MigrationStep>;
}

pub struct DataModelMigrationStepsInferrerImplWrapper {}

impl DataModelMigrationStepsInferrer for DataModelMigrationStepsInferrerImplWrapper {
    fn infer(&self, previous: &Datamodel, next: &Datamodel) -> Vec<MigrationStep> {
        let inferrer = DataModelMigrationStepsInferrerImpl { previous, next };
        inferrer.infer_internal()
    }
}

#[allow(dead_code)]
pub struct DataModelMigrationStepsInferrerImpl<'a> {
    previous: &'a Datamodel,
    next: &'a Datamodel,
}

// TODO: this does not deal with renames yet
impl<'a> DataModelMigrationStepsInferrerImpl<'a> {
    fn infer_internal(&self) -> Vec<MigrationStep> {
        let mut result: Vec<MigrationStep> = Vec::new();
        let models_to_create = self.models_to_create();
        let models_to_delete = self.models_to_delete();
        let models_to_update = self.models_to_update();
        let fields_to_create = self.fields_to_create();
        let fields_to_delete = self.fields_to_delete(&models_to_delete);
        let fields_to_update = self.fields_to_update();
        let enums_to_create = self.enums_to_create();
        let enums_to_delete = self.enums_to_delete();
        let enums_to_update = self.enums_to_update();

        result.append(&mut Self::wrap_as_step(models_to_create, MigrationStep::CreateModel));
        result.append(&mut Self::wrap_as_step(models_to_delete, MigrationStep::DeleteModel));
        result.append(&mut Self::wrap_as_step(models_to_update, MigrationStep::UpdateModel));
        result.append(&mut Self::wrap_as_step(fields_to_create, MigrationStep::CreateField));
        result.append(&mut Self::wrap_as_step(fields_to_delete, MigrationStep::DeleteField));
        result.append(&mut Self::wrap_as_step(fields_to_update, MigrationStep::UpdateField));
        result.append(&mut Self::wrap_as_step(enums_to_create, MigrationStep::CreateEnum));
        result.append(&mut Self::wrap_as_step(enums_to_delete, MigrationStep::DeleteEnum));
        result.append(&mut Self::wrap_as_step(enums_to_update, MigrationStep::UpdateEnum));
        result
    }

    fn models_to_create(&self) -> Vec<CreateModel> {
        let mut result = Vec::new();
        for next_model in self.next.models() {
            if !self.previous.has_model(&next_model.name()) {
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

    fn models_to_update(&self) -> Vec<UpdateModel> {
        let mut result = Vec::new();
        for previous_model in self.previous.models() {
            if let Some(next_model) = self.next.find_model(&previous_model.name) {
                let step = UpdateModel {
                    name: next_model.name.clone(),
                    new_name: None,
                    db_name: Self::diff(&previous_model.database_name, &next_model.database_name),
                    embedded: Self::diff(&previous_model.is_embedded, &next_model.is_embedded),
                };
                if step.is_any_option_set() {
                    result.push(step);
                }
            }
        }
        result
    }

    fn models_to_delete(&self) -> Vec<DeleteModel> {
        let mut result = Vec::new();
        for previous_model in self.previous.models() {
            if !self.next.has_model(&previous_model.name) {
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
            for next_field in next_model.fields() {
                let must_create_field = match self.previous.find_model(&next_model.name) {
                    None => true,
                    Some(previous_model) => previous_model.find_field(&next_field.name).is_none(),
                };
                if must_create_field {
                    let step = CreateField {
                        model: next_model.name.clone(),
                        name: next_field.name.clone(),
                        tpe: next_field.field_type.clone(),
                        arity: next_field.arity,
                        db_name: next_field.database_name.clone(),
                        default: next_field.default_value.clone(),
                        id: next_field.id_info.clone(),
                        is_created_at: None,
                        is_updated_at: None,
                        is_unique: next_field.is_unique,
                        scalar_list: next_field.scalar_list_strategy,
                    };
                    result.push(step);
                }
            }
        }
        result
    }

    fn fields_to_delete(&self, models_to_delete: &Vec<DeleteModel>) -> Vec<DeleteField> {
        let mut result = Vec::new();
        for previous_model in self.previous.models() {
            let model_is_deleted = models_to_delete
                .iter()
                .find(|dm| dm.name == previous_model.name)
                .is_none();
            if model_is_deleted {
                for previous_field in previous_model.fields() {
                    let must_delete_field = match self.next.find_model(&previous_model.name) {
                        None => true,
                        Some(next_model) => next_model.find_field(&previous_field.name).is_none(),
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
        }
        result
    }

    fn fields_to_update(&self) -> Vec<UpdateField> {
        let mut result = Vec::new();
        for previous_model in self.previous.models() {
            for previous_field in previous_model.fields() {
                if let Some(next_field) = self
                    .next
                    .find_model(&previous_model.name)
                    .and_then(|m| m.find_field(&previous_field.name))
                {
                    let (p, n) = (previous_field, next_field);
                    let step = UpdateField {
                        model: previous_model.name.clone(),
                        name: p.name.clone(),
                        new_name: None,
                        tpe: Self::diff(&p.field_type, &n.field_type),
                        arity: Self::diff(&p.arity, &n.arity),
                        db_name: Self::diff(&p.database_name, &n.database_name),
                        is_created_at: None,
                        is_updated_at: None,
                        is_unique: Self::diff(&p.is_unique, &n.is_unique),
                        id_info: None,
                        default: Self::diff(&p.default_value, &n.default_value),
                        scalar_list: Self::diff(&p.scalar_list_strategy, &n.scalar_list_strategy),
                    };
                    if step.is_any_option_set() {
                        result.push(step);
                    }
                }
            }
        }
        result
    }

    fn enums_to_create(&self) -> Vec<CreateEnum> {
        let mut result = Vec::new();
        for next_enum in self.next.enums() {
            if !self.previous.has_enum(&next_enum.name()) {
                let step = CreateEnum {
                    name: next_enum.name().to_string(),
                    db_name: next_enum.database_name.clone(),
                    values: next_enum.values.clone(),
                };
                result.push(step);
            }
        }

        result
    }

    fn enums_to_delete(&self) -> Vec<DeleteEnum> {
        let mut result = Vec::new();
        for previous_enum in self.previous.enums() {
            if !self.next.has_enum(&previous_enum.name) {
                let step = DeleteEnum {
                    name: previous_enum.name().clone(),
                };
                result.push(step);
            }
        }

        result
    }

    fn enums_to_update(&self) -> Vec<UpdateEnum> {
        let mut result = Vec::new();
        for previous_enum in self.previous.enums() {
            if let Some(next_enum) = self.next.find_enum(&previous_enum.name) {
                let step = UpdateEnum {
                    name: next_enum.name.clone(),
                    new_name: None,
                    db_name: Self::diff(&previous_enum.database_name, &next_enum.database_name),
                    values: Self::diff(&previous_enum.values, &next_enum.values),
                };
                if step.is_any_option_set() {
                    result.push(step);
                }
            }
        }
        result
    }

    fn diff<T: PartialEq + Clone>(current: &T, updated: &T) -> Option<T> {
        if current == updated {
            None
        } else {
            Some(updated.clone())
        }
    }

    fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<MigrationStep>
    where
        F: FnMut(T) -> MigrationStep,
    {
        steps.into_iter().map(|x| wrap_fn(x)).collect()
    }
}
