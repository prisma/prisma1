use datamodel::dml::*;
use migration_connector::steps::*;

pub fn create_field_step(model: &str, field: &str, scalar_type: ScalarType) -> MigrationStep {
    MigrationStep::CreateField(CreateField {
        model: model.to_string(),
        name: field.to_string(),
        tpe: FieldType::Base(scalar_type),
        arity: FieldArity::Required,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: false,
        id: None,
        default: None,
        scalar_list: None,
    })
}

pub fn delete_field_step(model: &str, field: &str) -> MigrationStep {
    MigrationStep::DeleteField(DeleteField {
        model: model.to_string(),
        name: field.to_string(),
    })
}

pub fn create_id_field_step(model: &str, field: &str, scalar_type: ScalarType) -> MigrationStep {
    MigrationStep::CreateField(CreateField {
        model: model.to_string(),
        name: field.to_string(),
        tpe: FieldType::Base(scalar_type),
        arity: FieldArity::Required,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: false,
        id: Some(IdInfo {
            strategy: IdStrategy::Auto,
            sequence: None,
        }),
        default: None,
        scalar_list: None,
    })
}

pub fn create_model_step(model: &str) -> MigrationStep {
    MigrationStep::CreateModel(CreateModel {
        name: model.to_string(),
        db_name: None,
        embedded: false,
    })
}
