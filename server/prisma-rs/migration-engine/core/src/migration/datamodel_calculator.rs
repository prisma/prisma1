use datamodel::*;
use migration_connector::steps::*;

// Macro to match all children in a parse tree
macro_rules! set (
    ($model:ident, $step:ident, $model_field:ident, $step_field:ident) => (
        if let Some(val) = &$step.$step_field {
            $model.$model_field = val.clone();
        }
    );
);

pub trait DataModelCalculator: std::panic::RefUnwindSafe {
    fn infer(&self, current: &Datamodel, steps: &Vec<MigrationStep>) -> Datamodel;
}

pub struct DataModelCalculatorImpl {}
impl DataModelCalculator for DataModelCalculatorImpl {
    fn infer(&self, current: &Datamodel, steps: &Vec<MigrationStep>) -> Datamodel {
        let mut result = current.clone();
        steps.into_iter().for_each(|step| match step {
            MigrationStep::DeleteModel(x) => apply_delete_model(&mut result, x),
            MigrationStep::UpdateModel(x) => apply_update_model(&mut result, x),
            MigrationStep::CreateModel(x) => apply_create_model(&mut result, x),
            MigrationStep::DeleteEnum(x) => apply_delete_enum(&mut result, x),
            MigrationStep::UpdateEnum(x) => apply_update_enum(&mut result, x),
            MigrationStep::CreateEnum(x) => apply_create_enum(&mut result, x),
            MigrationStep::DeleteField(x) => apply_delete_field(&mut result, x),
            MigrationStep::UpdateField(x) => apply_update_field(&mut result, x),
            MigrationStep::CreateField(x) => apply_create_field(&mut result, x),
        });
        result
    }
}

fn apply_delete_model(data_model: &mut Datamodel, step: &DeleteModel) {
    if !data_model.has_model(&step.name) {
        panic!(
            "The model {} does not exist in this Datamodel. It is not possible to delete it.",
            &step.name
        )
    }
    data_model.remove_model(&step.name);
}

fn apply_update_model(data_model: &mut Datamodel, step: &UpdateModel) {
    let model = data_model.find_model_mut(&step.name).expect(&format!(
        "The model {} does not exist in this Datamodel. It is not possible to update it.",
        &step.name
    ));

    set!(model, step, name, new_name);
    set!(model, step, is_embedded, embedded);
    set!(model, step, database_name, db_name);
}

fn apply_create_model(data_model: &mut Datamodel, step: &CreateModel) {
    if data_model.has_model(&step.name) {
        panic!(
            "The model {} already exists in this Datamodel. It is not possible to create it once more.",
            &step.name
        )
    }
    let mut model = Model::new(&step.name);
    model.is_embedded = step.embedded;
    model.database_name = step.db_name.clone();
    data_model.add_model(model);
}

fn apply_delete_enum(data_model: &mut Datamodel, step: &DeleteEnum) {
    if !data_model.has_enum(&step.name) {
        panic!(
            "The enum {} does not exist in this Datamodel. It is not possible to delete it.",
            &step.name
        )
    }
    data_model.remove_enum(&step.name);
}

fn apply_update_enum(data_model: &mut Datamodel, step: &UpdateEnum) {
    let model = data_model.find_enum_mut(&step.name).expect(&format!(
        "The enum {} does not exist in this Datamodel. It is not possible to update it.",
        &step.name,
    ));

    set!(model, step, name, new_name);
    set!(model, step, values, values);
    set!(model, step, database_name, db_name);
}

fn apply_create_enum(data_model: &mut Datamodel, step: &CreateEnum) {
    if data_model.has_enum(&step.name) {
        panic!(
            "The enum {} already exists in this Datamodel. It is not possible to create it once more.",
            &step.name
        )
    }
    let mut en = Enum::new(&step.name, step.values.clone());
    en.database_name = step.db_name.clone();
    data_model.add_enum(en);
}

fn apply_delete_field(data_model: &mut Datamodel, step: &DeleteField) {
    let model = data_model.models_mut().find(|m| m.name == step.model).expect(&format!(
        "The model {} does not exist in this Datamodel. It is not possible to delete a field in it.",
        step.model
    ));
    if model.find_field(&step.name).is_none() {
        panic!(
            "The field {} on model {} does not exist in this Datamodel. It is not possible to delete it.",
            &step.name, &step.model
        )
    }
    model.remove_field(&step.name);
}

fn apply_update_field(data_model: &mut Datamodel, step: &UpdateField) {
    let model = data_model.find_model_mut(&step.model).expect(&format!(
        "The model {} does not exist in this Datamodel. It is not possible to update a field in it.",
        step.model
    ));
    let field = model.find_field_mut(&step.name).expect(&format!(
        "The field {} on model {} does not exist in this Datamodel. It is not possible to update it.",
        &step.name, &step.model
    ));

    set!(field, step, name, new_name);
    set!(field, step, field_type, tpe);
    set!(field, step, arity, arity);
    set!(field, step, database_name, db_name);
    set!(field, step, id_info, id_info);
    set!(field, step, default_value, default);
    set!(field, step, scalar_list_strategy, scalar_list);
}

fn apply_create_field(data_model: &mut Datamodel, step: &CreateField) {
    let model = data_model.find_model_mut(&step.model).expect(&format!(
        "The model {} does not exist in this Datamodel. It is not possible to create a field in it.",
        step.model
    ));
    if model.find_field(&step.name).is_some() {
        panic!(
            "The field {} on model {} already exists in this Datamodel. It is not possible to create it once more.",
            &step.name, &step.model
        )
    }
    let mut field = Field::new(&step.name, step.tpe.clone());
    field.arity = step.arity;
    field.database_name = step.db_name.clone();
    field.default_value = step.default.clone();
    field.is_unique = step.is_unique;
    field.id_info = step.id.clone();
    field.scalar_list_strategy = step.scalar_list;

    model.add_field(field);
}
