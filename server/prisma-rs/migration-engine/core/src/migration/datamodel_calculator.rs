use datamodel::*;
use migration_connector::steps::*;

pub trait DataModelCalculator {
    fn infer(&self, current: &Schema, steps: Vec<MigrationStep>) -> Schema;
}

pub struct DataModelCalculatorImpl {}
impl DataModelCalculator for DataModelCalculatorImpl {
    fn infer(&self, current: &Schema, steps: Vec<MigrationStep>) -> Schema {
        let mut result = current.clone();
        steps.into_iter().for_each(|step| match step {
            MigrationStep::DeleteModel(x) => apply_delete_model(&mut result, x),
            MigrationStep::CreateModel(x) => apply_create_model(&mut result, x),
            MigrationStep::DeleteField(x) => apply_delete_field(&mut result, x),
            MigrationStep::CreateField(x) => apply_create_field(&mut result, x),
            _ => unimplemented!(),
        });
        result
    }
}

fn apply_delete_model(data_model: &mut Schema, step: DeleteModel) {
    data_model.remove_model(&step.name);
}

fn apply_create_model(data_model: &mut Schema, step: CreateModel) {
    let mut model = Model::new(&step.name);
    model.is_embedded = step.embedded;
    model.database_name = step.db_name;
    data_model.add_model(model);
}

fn apply_delete_field(data_model: &mut Schema, step: DeleteField) {
    let model = data_model.models_mut().find(|m| m.name == step.model).unwrap();
    model.remove_field(&step.name);
}

fn apply_create_field(data_model: &mut Schema, step: CreateField) {
    let model = data_model.models_mut().find(|m| m.name == step.model).unwrap();
    let mut field = Field::new(&step.name, step.tpe);
    field.arity = step.arity;
    field.database_name = step.db_name;
    field.default_value = step.default;
    field.is_unique = step.is_unique;
    field.id_info = step.id;
    field.scalar_list_strategy = step.scalar_list;

    model.add_field(field);
}
