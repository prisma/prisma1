use super::dmmf::*;
use crate::ast::Span;
use crate::dml;
use crate::dml::FromStrAndSpan;
use chrono::{DateTime, Utc};
use serde_json;

fn type_from_string(scalar: &str) -> dml::ScalarType {
    match scalar {
        "Int" => dml::ScalarType::Int,
        "Decimal" => dml::ScalarType::Decimal,
        "Float" => dml::ScalarType::Float,
        "Boolean" => dml::ScalarType::Boolean,
        "String" => dml::ScalarType::String,
        "DateTime" => dml::ScalarType::DateTime,
        _ => panic!(format!("Unknown scalar type {}.", scalar)),
    }
}

pub fn default_value_from_serde(
    container: &Option<serde_json::Value>,
    field_type: &dml::FieldType,
) -> Option<dml::Value> {
    match (container, field_type) {
        (Some(value), dml::FieldType::Base(scalar_type)) => Some(match (value, scalar_type) {
            (serde_json::Value::Bool(val), dml::ScalarType::Boolean) => dml::Value::Boolean(*val),
            (serde_json::Value::String(val), dml::ScalarType::String) => dml::Value::String(String::from(val.as_str())),
            (serde_json::Value::Number(val), dml::ScalarType::Float) => dml::Value::Float(val.as_f64().unwrap() as f32),
            (serde_json::Value::Number(val), dml::ScalarType::Int) => dml::Value::Int(val.as_i64().unwrap() as i32),
            (serde_json::Value::Number(val), dml::ScalarType::Decimal) => {
                dml::Value::Decimal(val.as_f64().unwrap() as f32)
            }
            (serde_json::Value::String(val), dml::ScalarType::DateTime) => {
                dml::Value::DateTime(String::from(val.as_str()).parse::<DateTime<Utc>>().unwrap())
            }
            _ => panic!("Invalid type/value combination for default value."),
        }),
        (Some(value), dml::FieldType::Enum(_)) => {
            Some(dml::Value::ConstantLiteral(String::from(value.as_str().unwrap())))
        }
        (Some(_), _) => panic!("Fields with non-scalar type cannot have default value"),
        _ => None,
    }
}

fn get_on_delete_strategy(strategy: &Option<String>) -> dml::OnDeleteStrategy {
    match strategy {
        Some(val) => dml::OnDeleteStrategy::from_str_and_span(&val, &Span::empty()).unwrap(),
        None => dml::OnDeleteStrategy::None,
    }
}

fn get_field_type(field: &Field) -> dml::FieldType {
    match &field.kind as &str {
        "object" => dml::FieldType::Relation(dml::RelationInfo {
            to: field.field_type.clone(),
            to_fields: field.relation_to_fields.clone().unwrap_or_default(),
            name: field.relation_name.clone(),
            on_delete: get_on_delete_strategy(&field.relation_on_delete),
        }),
        "enum" => dml::FieldType::Enum(field.field_type.clone()),
        "scalar" => dml::FieldType::Base(type_from_string(&field.field_type)),
        _ => panic!(format!("Unknown field kind {}.", &field.kind)),
    }
}

pub fn get_field_arity(is_required: bool, is_list: bool) -> dml::FieldArity {
    match (is_required, is_list) {
        (true, true) => dml::FieldArity::List,
        (false, true) => dml::FieldArity::List,
        (true, false) => dml::FieldArity::Required,
        (false, false) => dml::FieldArity::Optional,
    }
}

pub fn enum_from_dmmf(en: &Enum) -> dml::Enum {
    dml::Enum {
        name: en.name.clone(),
        values: en.values.clone(),
        database_name: en.db_name.clone(),
        comments: vec![],
    }
}

pub fn field_from_dmmf(field: &Field) -> dml::Field {
    let field_type = get_field_type(field);
    let default_value = default_value_from_serde(&field.default, &field_type);
    // TODO: Id details?
    let id_info = match &field.is_id {
        true => Some(dml::IdInfo {
            strategy: dml::IdStrategy::Auto,
            sequence: None,
        }),
        false => None,
    };

    dml::Field {
        name: field.name.clone(),
        arity: get_field_arity(field.is_required, field.is_list),
        database_name: field.db_name.clone(),
        field_type: field_type,
        default_value: default_value,
        id_info: id_info,
        is_unique: field.is_unique,
        // TODO: Scalar List Strategy
        scalar_list_strategy: None,
        is_generated: field.is_generated.unwrap_or(false),
        comments: vec![],
    }
}

pub fn model_from_dmmf(model: &Model) -> dml::Model {
    dml::Model {
        name: model.name.clone(),
        database_name: model.db_name.clone(),
        is_embedded: model.is_embedded,
        fields: model.fields.iter().map(&field_from_dmmf).collect(),
        comments: vec![],
    }
}

pub fn schema_from_dmmf(schema: &Datamodel) -> dml::Datamodel {
    let mut datamodel = dml::Datamodel {
        models: vec![],
        enums: vec![],
        comments: vec![],
    };

    for model in &schema.models {
        datamodel.add_model(model_from_dmmf(&model));
    }

    for enum_model in &schema.enums {
        datamodel.add_enum(enum_from_dmmf(&enum_model));
    }

    return datamodel;
}

pub fn parse_from_dmmf(dmmf: &str) -> dml::Datamodel {
    let parsed_dmmf = serde_json::from_str::<Datamodel>(&dmmf).expect("Failed to parse JSON");
    schema_from_dmmf(&parsed_dmmf)
}
