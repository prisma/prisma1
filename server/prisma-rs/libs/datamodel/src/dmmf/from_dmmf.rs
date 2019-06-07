use super::dmmf::*;
use crate::ast::Span;
use crate::common::FromStrAndSpan;
use crate::common::PrismaType;
use crate::{ast, dml, get_builtin_sources, source};
use chrono::{DateTime, Utc};
use serde_json;

fn type_from_string(scalar: &str) -> PrismaType {
    PrismaType::from_str_and_span(scalar, &crate::ast::Span::empty()).unwrap()
}

pub fn default_value_from_serde(
    container: &Option<serde_json::Value>,
    field_type: &dml::FieldType,
) -> Option<dml::Value> {
    match (container, field_type) {
        // Scalar.
        (Some(value), dml::FieldType::Base(scalar_type)) => Some(match (value, scalar_type) {
            (serde_json::Value::Bool(val), PrismaType::Boolean) => dml::Value::Boolean(*val),
            (serde_json::Value::String(val), PrismaType::String) => dml::Value::String(String::from(val.as_str())),
            (serde_json::Value::Number(val), PrismaType::Float) => dml::Value::Float(val.as_f64().unwrap() as f32),
            (serde_json::Value::Number(val), PrismaType::Int) => dml::Value::Int(val.as_i64().unwrap() as i32),
            (serde_json::Value::Number(val), PrismaType::Decimal) => dml::Value::Decimal(val.as_f64().unwrap() as f32),
            (serde_json::Value::String(val), PrismaType::DateTime) => {
                dml::Value::DateTime(String::from(val.as_str()).parse::<DateTime<Utc>>().unwrap())
            }
            // Function.
            (serde_json::Value::Object(_), _) => {
                let func = serde_json::from_value::<Function>(value.clone()).expect("Failed to parse function JSON");
                function_from_dmmf(&func, *scalar_type)
            }
            _ => panic!("Invalid type/value combination for default value."),
        }),
        // Enum.
        (Some(value), dml::FieldType::Enum(_)) => {
            Some(dml::Value::ConstantLiteral(String::from(value.as_str().unwrap())))
        }
        (Some(_), _) => panic!("Fields with non-scalar type cannot have default value"),
        _ => None,
    }
}

fn function_from_dmmf(func: &Function, expected_type: PrismaType) -> dml::Value {
    if func.args.len() > 0 {
        panic!("Function argument deserialization is not supported with DMMF. There are no type annotations yet, so it's not clear which is meant.");
    }

    if func.return_type != expected_type.to_string() {
        panic!(
            "Type missmatch during deserialization. Expected: {}, but got: {}.",
            expected_type.to_string(),
            func.return_type
        );
    }

    dml::Value::Expression(func.name.clone(), expected_type, vec![])
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
            name: field.relation_name.clone().unwrap_or(String::new()),
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
        is_updated_at: field.is_updated_at.unwrap_or(false),
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
        is_generated: model.is_generated.unwrap_or(false),
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

fn source_from_dmmf(source: &SourceConfig, loader: &source::SourceLoader) -> Box<source::Source> {
    // Loader only works on AST. We should change that.
    // TODO: This is code duplication with source serializer, the format is very similar.
    // Maybe we can impl the Source trait.
    let mut arguments: Vec<ast::Argument> = Vec::new();

    arguments.push(ast::Argument::new_string("type", &source.connector_type));
    arguments.push(ast::Argument::new_string("url", &source.url));

    let mut detail_arguments: Vec<ast::Argument> = Vec::new();

    for (key, value) in &source.config {
        detail_arguments.push(ast::Argument::new_string(&key, &value));
    }

    let ast_source = ast::SourceConfig {
        name: source.name.clone(),
        properties: arguments,
        detail_configuration: detail_arguments,
        comments: Vec::new(),
        span: ast::Span::empty(),
    };

    loader.load_source(&ast_source).expect("Source loading failed.")
}

pub fn sources_from_dmmf_with_plugins(
    dmmf: &str,
    source_definitions: Vec<Box<source::SourceDefinition>>,
) -> Vec<Box<source::Source>> {
    let dmmf_sources = serde_json::from_str::<Vec<SourceConfig>>(&dmmf).expect("Failed to parse JSON");
    let mut res = Vec::new();

    let mut source_loader = source::SourceLoader::new();
    for source in get_builtin_sources() {
        source_loader.add_source_definition(source);
    }
    for source in source_definitions {
        source_loader.add_source_definition(source);
    }

    for source in dmmf_sources {
        res.push(source_from_dmmf(&source, &source_loader))
    }

    res
}

pub fn sources_from_dmmf(dmmf: &str) -> Vec<Box<source::Source>> {
    sources_from_dmmf_with_plugins(dmmf, Vec::new())
}
