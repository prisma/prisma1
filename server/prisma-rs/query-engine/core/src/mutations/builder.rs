//! Providing an interface to build WriteQueries

use crate::{CoreError, CoreResult, WriteQuery};
use connector::mutaction::{CreateNode, TopLevelDatabaseMutaction};
use graphql_parser::query::{Field, Value};
use prisma_models::{ModelRef, PrismaArgs, PrismaValue, SchemaRef};

use rust_inflector::Inflector;

use std::collections::BTreeMap;
use std::sync::Arc;

/// A TopLevelMutation builder
///
/// It takes a graphql field and schema
/// and builds a mutation tree from it
#[derive(Debug)]
pub struct MutationBuilder<'field> {
    field: &'field Field,
    schema: SchemaRef,
}

impl<'field> MutationBuilder<'field> {
    pub fn new(schema: SchemaRef, field: &'field Field) -> Self {
        Self { field, schema }
    }

    pub fn build(self) -> CoreResult<WriteQuery> {
        let non_list_args = get_mutation_args(&self.field.arguments);
        let (op, model) = parse_model_action(
            self.field.alias.as_ref().unwrap_or_else(|| &self.field.name),
            Arc::clone(&self.schema),
        )?;

        let inner = match op {
            Operation::Create => TopLevelDatabaseMutaction::CreateNode(CreateNode {
                model,
                non_list_args,
                list_args: vec![],
                nested_mutactions: Default::default(),
            }),
            _ => unimplemented!(),
        };

        Ok(WriteQuery { inner, query: (), nested: vec![] })
    }
}

/// Extract String-Value pairs into usable mutation arguments
fn get_mutation_args(args: &Vec<(String, Value)>) -> PrismaArgs {
    args.iter()
        .fold(BTreeMap::new(), |mut map, (_, v)| {
            match v {
                Value::Object(o) => o.iter().for_each(|(k, v)| {
                    map.insert(k.clone(), PrismaValue::from_value(v));
                }),
                _ => panic!("Unknown argument structure!"),
            }
            map
        })
        .into()
}

/// A simple enum to discriminate top-level actions
#[allow(dead_code)] // FIXME: Remove!
enum Operation {
    Create,
    Update,
    Delete,
    Upsert,
    UpdateMany,
    DeleteMany,
    Reset,
}

impl From<&str> for Operation {
    fn from(s: &str) -> Self {
        match s {
            "create" => Operation::Create,
            _ => unimplemented!(),
        }
    }
}

/// Parse the mutation name into an action and the model it should operate on
fn parse_model_action(name: &String, schema: SchemaRef) -> CoreResult<(Operation, ModelRef)> {
    let actions = vec!["create"];

    let action = match actions.iter().find(|action| name.starts_with(*action)) {
        Some(a) => a,
        None => return Err(CoreError::QueryValidationError(format!("Unknown action: {}", name))),
    };
    let split: Vec<&str> = name.split(action).collect();
    let model_name = match split.get(1) {
        Some(mn) => mn,
        None => {
            return Err(CoreError::QueryValidationError(format!(
                "No model name for action {}",
                name
            )))
        }
    };

    let normalized = model_name.to_pascal_case();
    let model = match schema.models().iter().find(|m| m.name == normalized) {
        Some(m) => m,
        None => {
            return Err(CoreError::QueryValidationError(format!(
                "Model not found for mutation {}",
                name
            )))
        }
    };

    Ok((Operation::from(*action), Arc::clone(&model)))
}
