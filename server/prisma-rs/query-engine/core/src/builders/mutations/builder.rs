//! Providing an interface to build WriteQueries

use crate::{builders::utils, CoreError, CoreResult, WriteQuery};
use connector::mutaction::{
    CreateNode, DeleteNode, DeleteNodes, NestedMutactions, ResetData, TopLevelDatabaseMutaction, UpdateNode,
    UpdateNodes, UpsertNode,
};
use graphql_parser::query::{Field, Value};
use prisma_models::{InternalDataModelRef, ModelRef, PrismaArgs, PrismaValue, Project};

use crate::Inflector;
use rust_inflector::Inflector as RustInflector;

use std::collections::BTreeMap;
use std::sync::Arc;

/// A TopLevelMutation builder
///
/// It takes a graphql field and model
/// and builds a mutation tree from it
#[derive(Debug)]
pub struct MutationBuilder<'field> {
    field: &'field Field,
    model: InternalDataModelRef,
}

type PrismaListArgs = Vec<(String, Option<Vec<PrismaValue>>)>;

impl<'field> MutationBuilder<'field> {
    pub fn new(model: InternalDataModelRef, field: &'field Field) -> Self {
        Self { field, model }
    }

    pub fn build(self) -> CoreResult<WriteQuery> {
        // Handle `resetData` seperately
        if &self.field.name == "resetData" {
            return handle_reset(&self.field, &self.model);
        }

        let (non_list_args, list_args) = dbg!(get_mutation_args(&self.field.arguments));
        let (op, model) = parse_model_action(
            self.field.alias.as_ref().unwrap_or_else(|| &self.field.name),
            Arc::clone(&self.model),
        )?;

        // NestedCreateNode {
        //     relation_field: Arc<RelationField>,
        //     non_list_args: PrismaArgs,
        //     list_args: Vec<(String, PrismaListValue)>,
        //     top_is_create: bool,
        //     nested_mutactions: NestedMutactions,
        // }

        let inner =
            match op {
                Operation::Create => TopLevelDatabaseMutaction::CreateNode(CreateNode {
                    model: Arc::clone(&model),
                    non_list_args,
                    list_args,
                    nested_mutactions: build_nested(self.field, Arc::clone(&model), &op)?,
                }),
                Operation::Update => TopLevelDatabaseMutaction::UpdateNode(UpdateNode {
                    where_: utils::extract_node_selector(self.field, Arc::clone(&model))?,
                    non_list_args,
                    list_args,
                    nested_mutactions: build_nested(self.field, Arc::clone(&model), &op)?,
                }),
                Operation::UpdateMany => {
                    let query_args = utils::extract_query_args(self.field, Arc::clone(&model))?;
                    let filter = query_args.filter.map(|f| Ok(f)).unwrap_or_else(|| {
                        Err(CoreError::QueryValidationError("Required filters not found!".into()))
                    })?;

                    dbg!(&filter);

                    TopLevelDatabaseMutaction::UpdateNodes(UpdateNodes {
                        model: Arc::clone(&model),
                        filter,
                        non_list_args,
                        list_args,
                    })
                }
                Operation::Delete => TopLevelDatabaseMutaction::DeleteNode(DeleteNode {
                    where_: utils::extract_node_selector(self.field, Arc::clone(&model))?,
                }),
                Operation::DeleteMany => {
                    let query_args = utils::extract_query_args(self.field, Arc::clone(&model))?;
                    let filter = query_args.filter.map(|f| Ok(f)).unwrap_or_else(|| {
                        Err(CoreError::QueryValidationError("Required filters not found!".into()))
                    })?;

                    TopLevelDatabaseMutaction::DeleteNodes(DeleteNodes { model, filter })
                }
                Operation::Upsert => TopLevelDatabaseMutaction::UpsertNode(UpsertNode {
                    where_: utils::extract_node_selector(self.field, Arc::clone(&model))?,
                    create: CreateNode {
                        model: Arc::clone(&model),
                        non_list_args: non_list_args.clone(),
                        list_args: list_args.clone(),
                        nested_mutactions: build_nested(self.field, Arc::clone(&model), &op)?,
                    },
                    update: UpdateNode {
                        where_: utils::extract_node_selector(self.field, Arc::clone(&model))?,
                        non_list_args,
                        list_args,
                        nested_mutactions: build_nested(self.field, Arc::clone(&model), &op)?,
                    },
                }),
                _ => unimplemented!(),
            };

        // FIXME: Cloning is unethical and should be avoided
        Ok(WriteQuery {
            inner,
            field: self.field.clone(),
        })
    }
}

/// A trap-door function that handles `resetData` without doing a whole bunch of other stuff
fn handle_reset(field: &Field, data_model: &InternalDataModelRef) -> CoreResult<WriteQuery> {
    Ok(WriteQuery {
        inner: TopLevelDatabaseMutaction::ResetData(ResetData {
            project: Arc::new(Project::from(data_model)),
        }),
        field: field.clone(),
    })
}

/// Extract String-Value pairs into usable mutation arguments
#[allow(warnings)]
fn get_mutation_args(args: &Vec<(String, Value)>) -> (PrismaArgs, PrismaListArgs) {
    use crate::builders::{ScopedArg, ScopedArgNode};

    let scoped_args = dbg!(ScopedArg::parse(args));

    let (args, lists) = dbg!(args)
        .iter()
        .filter(|(arg, _)| arg.as_str() != "where") // `where` blocks are handled by filter logic!
        .fold((BTreeMap::new(), vec![]), |(mut map, mut vec), (_, v)| {
            match v {
                Value::Object(o) => o.iter().for_each(|(k, v)| {
                    match v {
                        // Deal with ScalarList initialisers
                        Value::Object(o) if o.contains_key("set") => {
                            vec.push((
                                k.clone(),
                                match o.get("set") {
                                    Some(Value::List(l)) => Some(
                                        l.iter()
                                            .map(|v| PrismaValue::from_value(v))
                                            .collect::<Vec<PrismaValue>>(),
                                    ),
                                    None => None,
                                    _ => unimplemented!(), // or unreachable? dunn duuuuun!
                                },
                            ));
                        }
                        // Deal with nested creates
                        Value::Object(o) if o.contains_key("create") => {}
                        // Deal with nested connects
                        Value::Object(o) if o.contains_key("connect") => {}
                        v => {
                            map.insert(k.clone(), PrismaValue::from_value(v));
                        }
                    }
                }),
                _ => panic!("Unknown argument structure!"),
            }

            (map, vec)
        });
    (args.into(), lists)
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
            "update" => Operation::Update,
            "updateMany" => Operation::UpdateMany,
            "delete" => Operation::Delete,
            "deleteMany" => Operation::DeleteMany,
            "upsert" => Operation::Upsert,
            _ => unimplemented!(),
        }
    }
}

/// Parse the mutation name into an action and the model it should operate on
fn parse_model_action(name: &String, model: InternalDataModelRef) -> CoreResult<(Operation, ModelRef)> {
    let actions = vec!["create", "updateMany", "update", "deleteMany", "delete", "upsert"];

    let action = match actions.iter().find(|action| name.starts_with(*action)) {
        Some(a) => a,
        None => return Err(CoreError::QueryValidationError(format!("Unknown action: {}", name))),
    };
    let split: Vec<&str> = name.split(action).collect();
    let model_name = match split.get(1) {
        Some(mn) => mn,
        None => {
            return Err(CoreError::QueryValidationError(format!(
                "No model name for action `{}`",
                name
            )))
        }
    };

    let normalized = dbg!(Inflector::singularize(model_name).to_pascal_case());
    let model = match model.models().iter().find(|m| m.name == normalized) {
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

/// Build nested mutations for a given field/model (called recursively)
fn build_nested(_field: &Field, _model: ModelRef, _top_level: &Operation) -> CoreResult<NestedMutactions> {
    Ok(Default::default())
}
