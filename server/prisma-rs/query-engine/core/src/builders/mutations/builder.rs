//! Providing an interface to build WriteQueries
#![allow(warnings)]

use crate::{
    builders::{convert_tree, utils, ScopedArg, ScopedArgNode},
    CoreError, CoreResult, WriteQuery,
};
use connector::mutaction::*; // ALL OF IT
use graphql_parser::query::{Field, Value};
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, Project};

use crate::Inflector;
use rust_inflector::Inflector as RustInflector;

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

impl<'field> MutationBuilder<'field> {
    pub fn new(model: InternalDataModelRef, field: &'field Field) -> Self {
        Self { field, model }
    }

    pub fn build(self) -> CoreResult<WriteQuery> {
        // Handle `resetData` seperately
        if &self.field.name == "resetData" {
            return handle_reset(&self.field, &self.model);
        }

        let args = dbg!(ScopedArg::parse(&self.field.arguments)?);
        let non_list_args = convert_tree(&args.data).into();
        let list_args = args.lists.iter().map(|la| la.into()).collect();

        let (op, model) = parse_model_action(
            self.field.alias.as_ref().unwrap_or_else(|| &self.field.name),
            Arc::clone(&self.model),
        )?;

        let nested_mutactions = build_nested(&args, Arc::clone(&model), &op)?;

        let inner =
            match op {
                Operation::Create => TopLevelDatabaseMutaction::CreateNode(CreateNode {
                    model: Arc::clone(&model),
                    non_list_args,
                    list_args,
                    nested_mutactions,
                }),
                Operation::Update => TopLevelDatabaseMutaction::UpdateNode(UpdateNode {
                    where_: utils::extract_node_selector(self.field, Arc::clone(&model))?,
                    non_list_args,
                    list_args,
                    nested_mutactions,
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
                        nested_mutactions: nested_mutactions.clone(),
                    },
                    update: UpdateNode {
                        where_: utils::extract_node_selector(self.field, Arc::clone(&model))?,
                        non_list_args,
                        list_args,
                        nested_mutactions,
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

    let normalized = Inflector::singularize(model_name).to_pascal_case();
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
fn build_nested<'f>(
    args: &'f ScopedArgNode<'f>,
    model: ModelRef,
    top_level: &Operation,
) -> CoreResult<NestedMutactions> {
    let mut nested: NestedMutactions = Default::default();

    // Filter out all non-nodes
    let nodes: Vec<_> = args
        .data
        .iter()
        .filter_map(|(k, v)| match v {
            ScopedArg::Node(node) => Some((k, node)),
            _ => None,
        })
        .collect();

    nodes.into_iter().for_each(|(name, val)| {
        let mut attrs = vec![];

        // Handle `create` arguments
        val.create.iter().for_each(|(i_key, i_val)| {
            match i_val {
                // Scalars are turned into non-list-arguments
                ScopedArg::Value(val) => {
                    attrs.push((i_key.clone(), val));
                }
                ScopedArg::Node(node) => {
                    // For every sub-node just append all creates
                    let inner = build_nested(&node, Arc::clone(&model), top_level).unwrap();
                    inner.creates.into_iter().for_each(|create| {
                        nested.creates.push(create);
                    });
                }
            }
        });

        // Now take the `create` arguments and create an actual `NestedCreateNode`
        // We do this here to end the recursion and pass up an actual value
        use std::collections::BTreeMap;

        let non_list_args = attrs
            .into_iter()
            .map(|(k, v)| (k, v.clone()))
            .collect::<BTreeMap<_, _>>()
            .into();

        let field = model.fields().find_from_all(&name);

        let relation_field = dbg!(match &field {
            Ok(ModelField::Relation(f)) => {
                let model = f.related_model();
                Arc::clone(&f)
            }
            _ => unreachable!(),
        });

        let ncn = NestedCreateNode {
            non_list_args,
            list_args: Default::default(),
            top_is_create: match top_level {
                Operation::Create => true,
                _ => false,
            },
            relation_field,
            nested_mutactions: Default::default(),
        };

        nested.creates.push(ncn);
    });

    dbg!(Ok(nested))
}
