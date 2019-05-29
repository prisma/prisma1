//! Providing an interface to build WriteQueries
#![allow(warnings)]

use crate::{
    builders::{convert_tree, utils, DataSet, NestedValue, ScopedArg, ScopedArgNode, ValueList, ValueMap, ValueSplit},
    CoreError, CoreResult, WriteQuery,
};
use connector::{filter::NodeSelector, mutaction::* /* ALL OF IT */};
use graphql_parser::query::{Field, Value};
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, PrismaValue, Project};

use crate::Inflector;
use rust_inflector::Inflector as RustInflector;

use std::{collections::BTreeMap, sync::Arc};

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

        let data = shift_data(&self.field.arguments);
        let ValueSplit { values, lists, nested } = ValueMap::init(&data).split();
        let non_list_args = values.to_prisma_values().into();
        let list_args = lists.into_iter().map(|la| la.convert()).collect();
        let raw_name = self.field.alias.as_ref().unwrap_or_else(|| &self.field.name).clone();

        let (op, model) = parse_model_action(
            &raw_name,
            Arc::clone(&self.model),
        )?;

        let nested_mutactions = build_nested_root(model.name.as_str(), &nested, Arc::clone(&model), &op)?;

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
            name: raw_name,
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
        name: "resetData".into(),
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

fn shift_data(from: &Vec<(String, Value)>) -> Vec<(String, Value)> {
    from.iter()
        .find(|(k, _)| k.as_str() == "data")
        .map(|(_, v)| match v {
            Value::Object(obj) => obj.iter().map(|(k, v)| (k.clone(), v.clone())).collect(),
            _ => unimplemented!(),
        })
        .unwrap()
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
        Some(mn) => mn.to_lowercase(),
        None => {
            return Err(CoreError::QueryValidationError(format!(
                "No model name for action `{}`",
                name
            )))
        }
    };

    let normalized = Inflector::singularize(&model_name).to_pascal_case();

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
fn build_nested_root<'f>(
    name: &'f str,
    args: &'f ValueMap,
    model: ModelRef,
    top_level: &Operation,
) -> CoreResult<NestedMutactions> {

    let mut collection = NestedMutactions::default();

    let eval = args.eval_tree();

    for value in eval.into_iter() {
        match value {
            NestedValue::Simple { name, kind, map } => {
                let ValueSplit { values, lists, nested } = map.split();

                let field = model.fields().find_from_all(&name);
                let relation_field = match &field {
                    Ok(ModelField::Relation(f)) => {
                        let model = f.related_model();
                        Arc::clone(&f)
                    }
                    _ => unimplemented!(),
                };

                let model = Arc::clone(&relation_field.related_model());
                let non_list_args = values.to_prisma_values().into();
                let list_args = lists.into_iter().map(|la| la.convert()).collect();
                let nested_mutactions = build_nested_root(&name, &nested, model, top_level)?;

                match kind.as_str() {
                    "create" => {
                        collection.creates.push(NestedCreateNode {
                            non_list_args,
                            list_args,
                            top_is_create: match top_level {
                                Operation::Create => true,
                                _ => false,
                            },
                            relation_field,
                            nested_mutactions,
                        });
                    }
                    _ => unimplemented!(),
                }
            }
            NestedValue::Many { name, kind, list } => {
                let field = model.fields().find_from_all(&name).unwrap();
                let (relation_field, relation_model) = match &field {
                    ModelField::Relation(f) => {
                        (Arc::clone(&f), f.related_model())
                    }
                    _ => unimplemented!(),
                };

                match kind.as_str() {
                    "connect" => {

                        // Create a connect for every map
                        for obj in list.into_iter() {
                            // Get the first valid field name that is a scalar
                            let where_ = obj.to_node_selector(Arc::clone(&model))?;

                            collection.connects.push(NestedConnect {
                                relation_field: Arc::clone(&relation_field),
                                where_,
                                top_is_create: match top_level {
                                    Operation::Create => true,
                                    _ => false,
                                },
                            });
                        }
                    }
                    "updateMany" => {
                        for obj in list.into_iter() {
                            let filter = utils::extract_query_args_inner(
                                obj.0
                                    .iter()
                                    .filter(|(arg, _)| arg.as_str() != "data")
                                    .map(|(a, b)| (a, b)),
                                Arc::clone(&relation_model),
                            )?
                            .filter;

                            let ValueSplit { values, lists, nested } = obj.split();
                            let non_list_args = values.to_prisma_values().into();
                            let list_args = lists.into_iter().map(|la| la.convert()).collect();

                            collection.update_manys.push(NestedUpdateNodes {
                                relation_field: Arc::clone(&relation_field),
                                filter,
                                non_list_args,
                                list_args,
                            });
                        }
                    },
                    "create" => {
                        for obj in list.into_iter() {
                            let ValueSplit { values, lists, nested } = obj.split();
                            let non_list_args = values.to_prisma_values().into();
                            let list_args = lists.into_iter().map(|la| la.convert()).collect();
                            let nested_mutactions = build_nested_root(&name, &nested, Arc::clone(&model), top_level)?;

                            collection.creates.push(NestedCreateNode {
                                non_list_args,
                                list_args,
                                top_is_create: match top_level {
                                    Operation::Create => true,
                                    _ => false,
                                },
                                relation_field: Arc::clone(&relation_field),
                                nested_mutactions,
                            });
                        }
                    }
                    cmd => panic!("Not yet implemented `{}`!", cmd),
                }
            }
            NestedValue::Upsert { name, create, update } => unimplemented!(),
        }
    }

    Ok(collection)
}
