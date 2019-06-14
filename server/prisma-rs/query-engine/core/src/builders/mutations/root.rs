//! Providing an interface to build WriteQueries
#![allow(warnings)]

use crate::{
    builders::{utils, NestedValue, ValueList, ValueMap, ValueSplit},
    CoreError, CoreResult, ManyNestedBuilder, QuerySchemaRef, SimpleNestedBuilder, UpsertNestedBuilder, WriteQuery,
};
use crate::{
    schema::{ModelOperation, OperationTag},
    Inflector, Query,
};
use connector::{filter::NodeSelector, mutaction::* /* ALL OF IT */};
use graphql_parser::query::{Field, Value};
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, PrismaValue, Project};
use rust_inflector::Inflector as RustInflector;
use std::{collections::BTreeMap, sync::Arc};

/// A TopLevelMutation builder
///
/// It takes a graphql field and model
/// and builds a mutation tree from it
#[derive(Debug)]
pub struct MutationBuilder<'field> {
    field: &'field Field,
    query_schema: QuerySchemaRef,
}

impl<'field> MutationBuilder<'field> {
    pub fn new(query_schema: QuerySchemaRef, field: &'field Field) -> Self {
        Self { field, query_schema }
    }

    pub fn build(self) -> CoreResult<WriteQuery> {
        // Handle `resetData` separately
        if &self.field.name == "resetData" {
            return handle_reset(&self.field, Arc::clone(&self.query_schema.internal_data_model));
        }

        let args = into_tree(&self.field.arguments);
        let raw_name = self.field.alias.as_ref().unwrap_or_else(|| &self.field.name).clone();
        let model_operation = parse_model_action(&raw_name, Arc::clone(&self.model))?;

        let inner = match model_operation.operation {
            OperationTag::CreateSingle => {
                let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "data")?).split();
                let non_list_args = values.to_prisma_values().into();
                let list_args = lists.into_iter().map(|la| la.convert()).collect();
                let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), &op)?;

                TopLevelDatabaseMutaction::CreateNode(CreateNode {
                    model: Arc::clone(&model),
                    non_list_args,
                    list_args,
                    nested_mutactions,
                })
            }
            OperationTag::UpdateSingle => {
                let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "data")?).split();
                let non_list_args = values.to_prisma_values().into();
                let list_args = lists.into_iter().map(|la| la.convert()).collect();
                let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), &op)?;

                let where_ = ValueMap(shift_data(&args, "where")?)
                    .to_node_selector(Arc::clone(&model))
                    .map_or(
                        Err(CoreError::QueryValidationError("No `where` on connect".into())),
                        |w| Ok(w),
                    )?;

                TopLevelDatabaseMutaction::UpdateNode(UpdateNode {
                    where_,
                    non_list_args,
                    list_args,
                    nested_mutactions,
                })
            }
            OperationTag::UpdateMany => {
                let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "data")?).split();
                let non_list_args = values.to_prisma_values().into();
                let list_args = lists.into_iter().map(|la| la.convert()).collect();
                let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), &op)?;

                let query_args = utils::extract_query_args(self.field, Arc::clone(&model))?;
                let filter = query_args
                    .filter
                    .map(|f| Ok(f))
                    .unwrap_or_else(|| Err(CoreError::QueryValidationError("Required filters not found!".into())))?;

                TopLevelDatabaseMutaction::UpdateNodes(UpdateNodes {
                    model: Arc::clone(&model),
                    filter,
                    non_list_args,
                    list_args,
                })
            }
            OperationTag::DeleteSingle => {
                let where_ = ValueMap(shift_data(&args, "where")?)
                    .to_node_selector(Arc::clone(&model))
                    .map_or(
                        Err(CoreError::QueryValidationError("No `where` on connect".into())),
                        |w| Ok(w),
                    )?;

                TopLevelDatabaseMutaction::DeleteNode(DeleteNode { where_ })
            }
            OperationTag::DeleteMany => {
                let query_args = utils::extract_query_args(self.field, Arc::clone(&model))?;
                let filter = query_args
                    .filter
                    .map(|f| Ok(f))
                    .unwrap_or_else(|| Err(CoreError::QueryValidationError("Required filters not found!".into())))?;

                TopLevelDatabaseMutaction::DeleteNodes(DeleteNodes { model, filter })
            }
            OperationTag::UpsertSingle => {
                let where_ = utils::extract_node_selector(self.field, Arc::clone(&model))?;

                let create = {
                    let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "create")?).split();
                    let non_list_args = values.to_prisma_values().into();
                    let list_args = lists.into_iter().map(|la| la.convert()).collect();
                    let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), &op)?;
                    let model = Arc::clone(&model);

                    CreateNode {
                        model,
                        non_list_args,
                        list_args,
                        nested_mutactions,
                    }
                };

                let update = {
                    let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "update")?).split();
                    let non_list_args = values.to_prisma_values().into();
                    let list_args = lists.into_iter().map(|la| la.convert()).collect();
                    let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), &op)?;
                    let where_ = where_.clone();

                    UpdateNode {
                        where_,
                        non_list_args,
                        list_args,
                        nested_mutactions,
                    }
                };

                TopLevelDatabaseMutaction::UpsertNode(UpsertNode { where_, create, update })
            }
            _ => unimplemented!(),
        };

        // FIXME: Cloning is unethical and should be avoided
        Ok(WriteQuery {
            inner: dbg!(inner),
            name: raw_name,
            field: self.field.clone(),
        })
    }
}

/// A trap-door function that handles `resetData` without doing a whole bunch of other stuff
fn handle_reset(field: &Field, internal_data_model: InternalDataModelRef) -> CoreResult<WriteQuery> {
    Ok(WriteQuery {
        inner: TopLevelDatabaseMutaction::ResetData(ResetData { internal_data_model }),
        name: "resetData".into(),
        field: field.clone(),
    })
}

/// Convert arguments provided by graphql-ast into a tree
fn into_tree(from: &Vec<(String, Value)>) -> BTreeMap<String, Value> {
    from.into_iter().map(|(a, b)| (a.clone(), b.clone())).collect()
}

/// Shift into a sub-tree of arguments
fn shift_data(from: &BTreeMap<String, Value>, idx: &str) -> CoreResult<BTreeMap<String, Value>> {
    from.get(idx).map_or(
        Err(CoreError::QueryValidationError(format!(
            "Failed to resolve `{}` block!",
            idx
        ))),
        |c| match c {
            Value::Object(obj) => Ok(obj.clone()),
            child => Err(CoreError::QueryValidationError(format!(
                "Invalid child type for `{}`: `{}`",
                idx, child
            ))),
        },
    )
}

/// Parse the mutation name into an operation to perform.
fn parse_model_action(name: &String, query_schema: QuerySchemaRef) -> CoreResult<ModelOperation> {
    //    let actions = vec!["create", "updateMany", "update", "deleteMany", "delete", "upsert"];
    //    let action = match actions.iter().find(|action| name.starts_with(*action)) {
    //        Some(a) => a,
    //        None => return Err(CoreError::QueryValidationError(format!("Unknown action: {}", name))),
    //    };

    //    let split: Vec<&str> = name.split(action).collect();

    query_schema.find_mutation_field(name);

    let model_name = match split.get(1) {
        Some(mn) => mn.to_lowercase(),
        None => {
            return Err(CoreError::QueryValidationError(format!(
                "No model name for action `{}`",
                name
            )))
        }
    };

    //    // FIXME: This is required because our `to_pascal_case` inflector works differently
    //    let normalized = match Inflector::singularize(&model_name).as_str() {
    //        "scalarmodel" => "ScalarModel".into(),
    //        name => name.to_pascal_case(),
    //    };

    //    println!("{} ==> {}", &model_name, &normalized);

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
pub(crate) fn build_nested_root<'f>(
    name: &'f str,
    args: &'f ValueMap,
    model: ModelRef,
    top_level: &Operation,
) -> CoreResult<NestedMutactions> {
    let mut collection = NestedMutactions::default();
    let eval = args.eval_tree(model.name.as_str());

    for value in eval.into_iter() {
        match value {
            NestedValue::Simple { name, kind, map } => {
                SimpleNestedBuilder::build(name, kind, map, &mut collection, Arc::clone(&model), None, top_level)?
            }
            NestedValue::Block {
                name,
                kind,
                data,
                where_,
            } => SimpleNestedBuilder::build(
                name,
                kind,
                data,
                &mut collection,
                Arc::clone(&model),
                Some(where_),
                top_level,
            )?,
            NestedValue::Many { name, kind, list } => ManyNestedBuilder::build(
                name,
                kind,
                list.into_iter(),
                &mut collection,
                Arc::clone(&model),
                top_level,
            )?,
            NestedValue::Upsert {
                name,
                create,
                update,
                where_,
            } => UpsertNestedBuilder::build(
                name,
                where_,
                create,
                update,
                &mut collection,
                Arc::clone(&model),
                top_level,
            )?,
            _ => unimplemented!(),
        };
    }

    Ok(collection)
}
