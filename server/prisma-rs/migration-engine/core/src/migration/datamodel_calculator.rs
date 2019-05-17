use datamodel::*;
use migration_connector::steps::*;

// Macro to match all children in a parse tree
macro_rules! set (
    ($model:ident, $step:ident, $model_field:ident, $step_field:ident) => (
        if let Some(val) = $step.$step_field {
            $model.$model_field = val;
        }
    );
);

pub trait DataModelCalculator: std::panic::RefUnwindSafe {
    fn infer(&self, current: &Schema, steps: Vec<MigrationStep>) -> Schema;
}

pub struct DataModelCalculatorImpl {}
impl DataModelCalculator for DataModelCalculatorImpl {
    fn infer(&self, current: &Schema, steps: Vec<MigrationStep>) -> Schema {
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

fn apply_delete_model(data_model: &mut Schema, step: DeleteModel) {
    data_model.remove_model(&step.name);
}

fn apply_update_model(data_model: &mut Schema, step: UpdateModel) {
    let model = data_model.find_model_mut(&step.name).unwrap();

    set!(model, step, name, new_name);
    set!(model, step, is_embedded, embedded);
    set!(model, step, database_name, db_name);
}

fn apply_create_model(data_model: &mut Schema, step: CreateModel) {
    let mut model = Model::new(&step.name);
    model.is_embedded = step.embedded;
    model.database_name = step.db_name;
    data_model.add_model(model);
}

fn apply_delete_enum(data_model: &mut Schema, step: DeleteEnum) {
    data_model.remove_enum(&step.name);
}

fn apply_update_enum(data_model: &mut Schema, step: UpdateEnum) {
    let model = data_model.find_enum_mut(&step.name).unwrap();

    set!(model, step, name, new_name);
    set!(model, step, values, values);
    set!(model, step, database_name, db_name);
}

fn apply_create_enum(data_model: &mut Schema, step: CreateEnum) {
    let mut en = Enum::new(&step.name, step.values);
    en.database_name = step.db_name;
    data_model.add_enum(en);
}

fn apply_delete_field(data_model: &mut Schema, step: DeleteField) {
    let model = data_model.models_mut().find(|m| m.name == step.model).unwrap();
    model.remove_field(&step.name);
}

fn apply_update_field(data_model: &mut Schema, step: UpdateField) {
    let model = data_model.find_model_mut(&step.model).unwrap();
    let field = model.find_field_mut(&step.name).unwrap();

    set!(field, step, name, new_name);
    set!(field, step, field_type, tpe);
    set!(field, step, arity, arity);
    set!(field, step, database_name, db_name);
    set!(field, step, id_info, id_info);
    set!(field, step, default_value, default);
    set!(field, step, scalar_list_strategy, scalar_list);
}

fn apply_create_field(data_model: &mut Schema, step: CreateField) {
    let model = data_model.find_model_mut(&step.model).unwrap();
    let mut field = Field::new(&step.name, step.tpe);
    field.arity = step.arity;
    field.database_name = step.db_name;
    field.default_value = step.default;
    field.is_unique = step.is_unique;
    field.id_info = step.id;
    field.scalar_list_strategy = step.scalar_list;

    model.add_field(field);
}
