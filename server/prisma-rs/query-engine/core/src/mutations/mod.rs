//! Mutation builder module
#![warn(warnings)]

use graphql_parser::query::{Value, Field, OperationDefinition};
use prisma_models::{SchemaRef, ModelRef, SelectedFields, PrismaArgs, PrismaValue};
use crate::{CoreError, CoreResult};
use connector::{DatabaseMutactionExecutor, mutaction::TopLevelDatabaseMutaction};
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

/// A small wrapper around a write query
#[derive(Debug)]
pub struct WriteQuery {
    root: TopLevelDatabaseMutaction,

    /// This is the selection-set for the following query
    // TODO: Change this to a tree
    query: OperationDefinition,

    /// Nested mutations
    nested: (),
}

pub struct WriteQueryExecutor {
    pub write_executor: Arc<DatabaseMutactionExecutor + Send + Sync + 'static>,
}

impl<'field> MutationBuilder<'field> {
    pub fn new(schema: SchemaRef, field: &'field Field) -> Self {
        Self { field, schema }
    }

    pub fn build(self) -> CoreResult<WriteQuery> {
        unimplemented!()
    }
}

/// Extract String-Value pairs into usable mutation arguments
fn get_mutation_args(args: Vec<(String, Value)>) -> PrismaArgs {
    args.into_iter().fold(BTreeMap::new(), |mut map, (k, v)| {
        map.insert(k, PrismaValue::from_value(&v));
        map
    }).into()
}

/// A simple enum to discriminate top-level actions
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
            _ => unimplemented!()
        }
    }
}

fn parse_model_action(name: &String, schema: SchemaRef) -> CoreResult<(ModelRef, Operation)> {
    let actions = vec![ "create" ];

    let action = match actions.iter().find(|action| name.starts_with(*action)) {
        Some(a) => a,
        None => return Err(CoreError::QueryValidationError(
            format!("Unknown action: {}", name)
        )),
    };
    let split: Vec<&str> = name.split(action).collect();
    let model_name = match split.get(1) {
        Some(mn) => mn,
        None => return Err(CoreError::QueryValidationError(
            format!("No model name for action {}", name)
        )),
    };

    unimplemented!()
}