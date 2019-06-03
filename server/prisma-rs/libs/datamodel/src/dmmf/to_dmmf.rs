use super::dmmf::*;
use crate::dml;
use crate::source::Source;
use serde_json;

fn get_field_kind(field: &dml::Field) -> String {
    match field.field_type {
        dml::FieldType::Relation(_) => String::from("relation"),
        dml::FieldType::Enum(_) => String::from("enum"),
        dml::FieldType::Base(_) => String::from("scalar"),
        _ => unimplemented!("DMMF does not support field type {:?}", field.field_type),
    }
}

fn type_to_string(scalar: &dml::ScalarType) -> String {
    match scalar {
        dml::ScalarType::Int => String::from("Int"),
        dml::ScalarType::Decimal => String::from("Decimal"),
        dml::ScalarType::Float => String::from("Float"),
        dml::ScalarType::Boolean => String::from("Boolean"),
        dml::ScalarType::String => String::from("String"),
        dml::ScalarType::DateTime => String::from("DateTime"),
    }
}

fn get_field_type(field: &dml::Field) -> String {
    match &field.field_type {
        dml::FieldType::Relation(relation_info) => relation_info.to.clone(),
        dml::FieldType::Enum(t) => t.clone(),
        dml::FieldType::Base(t) => type_to_string(t),
        dml::FieldType::ConnectorSpecific {
            base_type: t,
            connector_type: _,
        } => type_to_string(t),
    }
}

pub fn enum_to_dmmf(en: &dml::Enum) -> Enum {
    Enum {
        name: en.name.clone(),
        values: en.values.clone(),
        db_name: en.database_name.clone(),
    }
}

pub fn default_value_to_serde(container: &Option<dml::Value>) -> Option<serde_json::Value> {
    match container {
        Some(value) => Some(match value {
            dml::Value::Boolean(val) => serde_json::Value::Bool(*val),
            dml::Value::String(val) => serde_json::Value::String(val.clone()),
            dml::Value::ConstantLiteral(val) => serde_json::Value::String(val.clone()),
            dml::Value::Float(val) => serde_json::Value::Number(serde_json::Number::from_f64(*val as f64).unwrap()),
            dml::Value::Int(val) => serde_json::Value::Number(serde_json::Number::from_f64(*val as f64).unwrap()),
            dml::Value::Decimal(val) => serde_json::Value::Number(serde_json::Number::from_f64(*val as f64).unwrap()),
            dml::Value::DateTime(val) => serde_json::Value::String(val.to_rfc3339()),
        }),
        None => None,
    }
}

pub fn get_relation_name(field: &dml::Field) -> Option<String> {
    match &field.field_type {
        dml::FieldType::Relation(relation_info) => relation_info.name.clone(),
        _ => None,
    }
}

pub fn get_relation_to_fields(field: &dml::Field) -> Option<Vec<String>> {
    match &field.field_type {
        dml::FieldType::Relation(relation_info) => Some(relation_info.to_fields.clone()),
        _ => None,
    }
}

pub fn get_relation_delete_strategy(field: &dml::Field) -> Option<String> {
    match &field.field_type {
        dml::FieldType::Relation(relation_info) => Some(relation_info.on_delete.to_string()),
        _ => None,
    }
}

pub fn field_to_dmmf(field: &dml::Field) -> Field {
    Field {
        name: field.name.clone(),
        kind: get_field_kind(field),
        db_name: field.database_name.clone(),
        is_required: field.arity == dml::FieldArity::Required,
        is_list: field.arity == dml::FieldArity::List,
        is_id: field.id_info.is_some(),
        default: default_value_to_serde(&field.default_value),
        is_unique: field.is_unique,
        relation_name: get_relation_name(field),
        relation_to_fields: get_relation_to_fields(field),
        relation_on_delete: get_relation_delete_strategy(field),
        field_type: get_field_type(field),
        is_generated: Some(field.is_generated),
    }
}

pub fn model_to_dmmf(model: &dml::Model) -> Model {
    Model {
        name: model.name.clone(),
        db_name: model.database_name.clone(),
        is_embedded: model.is_embedded,
        fields: model.fields().map(&field_to_dmmf).collect(),
    }
}

pub fn schema_to_dmmf(schema: &dml::Datamodel) -> Datamodel {
    let mut datamodel = Datamodel {
        models: vec![],
        enums: vec![],
    };

    for model in schema.models() {
        datamodel.models.push(model_to_dmmf(&model));
    }

    for enum_model in schema.enums() {
        datamodel.enums.push(enum_to_dmmf(&enum_model));
    }

    return datamodel;
}

pub fn render_to_dmmf(schema: &dml::Datamodel) -> String {
    let dmmf = schema_to_dmmf(schema);
    return serde_json::to_string_pretty(&dmmf).expect("Failed to render JSON");
}

pub fn render_to_dmmf_value(schema: &dml::Datamodel) -> serde_json::Value {
    let dmmf = schema_to_dmmf(schema);
    return serde_json::to_value(&dmmf).expect("Failed to render JSON");
}

fn source_to_dmmf(source: &Box<Source>) -> SourceConfig {
    SourceConfig {
        name: source.name().clone(),
        connector_type: String::from(source.connector_type()),
        url: source.url().clone(),
        config: source.config().clone(),
    }
}

pub fn render_config_to_dmmf(sources: &Vec<Box<Source>>) -> String {
    let mut res: Vec<SourceConfig> = Vec::new();

    for source in sources {
        res.push(source_to_dmmf(source));
    }

    return serde_json::to_string_pretty(&res).expect("Failed to render JSON");
}
