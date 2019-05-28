use crate::dml;
use serde;
use serde_json;

// This is a simple JSON serialization using Serde.
// The JSON format follows the DMMF spec, but is incomplete.

#[derive(Debug, serde::Serialize)]
pub struct Field {
    pub name: String,
    pub kind: String,
    pub dbName: Option<String>,
    pub arity: String,
    pub isUnique: bool,
    #[serde(rename = "type")]
    pub field_type: String,
}

#[derive(Debug, serde::Serialize)]
pub struct Model {
    pub isEnum: bool,
    pub name: String,
    pub isEmbedded: bool,
    pub dbName: Option<String>,
    pub fields: Vec<Field>,
}

#[derive(Debug, serde::Serialize)]
pub struct Enum {
    pub isEnum: bool,
    pub name: String,
    pub values: Vec<String>,
}

#[derive(Debug, serde::Serialize)]
pub struct Datamodel {
    pub models: Vec<serde_json::Value>,
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
        dml::ScalarType::Enum => panic!("Enum is an internally used type and should never be rendered."),
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

fn get_field_arity(field: &dml::Field) -> String {
    match field.arity {
        dml::FieldArity::Required => String::from("required"),
        dml::FieldArity::Optional => String::from("optional"),
        dml::FieldArity::List => String::from("list"),
    }
}

pub fn enum_to_dmmf(en: &dml::Enum) -> Enum {
    Enum {
        name: en.name.clone(),
        values: en.values.clone(),
        isEnum: true,
    }
}

pub fn field_to_dmmf(field: &dml::Field) -> Field {
    Field {
        name: field.name.clone(),
        kind: get_field_kind(field),
        dbName: field.database_name.clone(),
        arity: get_field_arity(field),
        isUnique: field.is_unique,
        field_type: get_field_type(field),
    }
}

pub fn model_to_dmmf(model: &dml::Model) -> Model {
    Model {
        name: model.name.clone(),
        dbName: model.database_name.clone(),
        isEmbedded: model.is_embedded,
        fields: model.fields().map(&field_to_dmmf).collect(),
        isEnum: false,
    }
}

pub fn schema_to_dmmf(schema: &dml::Schema) -> Datamodel {
    let mut datamodel = Datamodel { models: vec![] };

    for model in schema.models() {
        datamodel
            .models
            .push(serde_json::to_value(&model_to_dmmf(&model)).expect("Failed to render enum"))
    }

    for enum_model in schema.enums() {
        datamodel
            .models
            .push(serde_json::to_value(&enum_to_dmmf(&enum_model)).expect("Failed to render enum"))
    }

    return datamodel;
}

pub fn render_to_dmmf(schema: &dml::Schema) -> String {
    let dmmf = schema_to_dmmf(schema);

    return serde_json::to_string_pretty(&dmmf).expect("Failed to render JSON");
}
