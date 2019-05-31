use crate::dml;
use serde;
use serde_json;

/// This is a partial implementation of the DMMF format.
/// No longer maintained. Don't use.

// This is a simple JSON serialization using Serde.
// The JSON format follows the DMMF spec, but is incomplete.

#[serde(rename_all = "camelCase")]
#[derive(Debug, serde::Serialize)]
pub struct Field {
    pub name: String,
    pub kind: String,
    pub db_name: Option<String>,
    pub is_list: bool,
    pub is_required: bool,
    pub is_unique: bool,
    pub is_id: bool,
    #[serde(rename = "type")]
    pub field_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<serde_json::Value>,
}

#[serde(rename_all = "camelCase")]
#[derive(Debug, serde::Serialize)]
pub struct Model {
    pub name: String,
    pub is_embedded: bool,
    pub db_name: Option<String>,
    pub fields: Vec<Field>,
}

#[serde(rename_all = "camelCase")]
#[derive(Debug, serde::Serialize)]
pub struct Enum {
    pub name: String,
    pub values: Vec<String>,
}

#[derive(Debug, serde::Serialize)]
pub struct Datamodel {
    pub enums: Vec<Enum>,
    pub models: Vec<Model>,
}

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
        field_type: get_field_type(field),
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
