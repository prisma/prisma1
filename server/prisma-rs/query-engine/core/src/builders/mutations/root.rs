//! Providing an interface to build WriteQueries
#![allow(warnings)]

use crate::{
    builders::{utils, LookAhead, NestedValue, ValueList, ValueMap, ValueSplit},
    extend_defaults,
    schema::{ModelOperation, OperationTag},
    CoreError, CoreResult, FieldRef, InputObjectTypeStrongRef, InputType, IntoArc, ManyNestedBuilder, QuerySchemaRef,
    SimpleNestedBuilder, UpsertNestedBuilder, WriteQuery, WriteQuerySet,
};
use connector::{filter::NodeSelector, mutaction::* /* ALL OF IT */};
use graphql_parser::query::{Field, Value};
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, PrismaArgs, PrismaValue, Project};
use std::{collections::BTreeMap, sync::Arc};

/// A root mutation builder
///
/// It takes a GraphQL field and model
/// and builds a mutation tree from it.
#[derive(Debug)]
pub struct RootMutationBuilder<'field> {
    field: &'field Field,
    query_schema: QuerySchemaRef,
}

impl<'field> RootMutationBuilder<'field> {
    pub fn new(query_schema: QuerySchemaRef, field: &'field Field) -> Self {
        Self { field, query_schema }
    }

    pub fn build(self) -> CoreResult<WriteQuerySet> {
        // Handle `resetData` separately
        if &self.field.name == "resetData" {
            return handle_reset(&self.field, Arc::clone(&self.query_schema.internal_data_model));
        }

        let args = into_tree(&self.field.arguments);
        let raw_name = self.field.alias.as_ref().unwrap_or_else(|| &self.field.name).clone();
        let schema_field = find_field(raw_name.as_str(), Arc::clone(&self.query_schema))?;
        let model_operation = schema_field
            .operation
            .expect("Expected top level field to have an associated model operation.")
            .clone();

        let model = Arc::clone(&model_operation.model);

        fn arg_object(name: &str, sf: FieldRef) -> CoreResult<InputObjectTypeStrongRef> {
            let arg = sf
                .arguments
                .iter()
                .find(|arg| arg.name == name)
                .expect(&format!("Missing {} argument.", name));

            match &arg.argument_type {
                InputType::Object(obj) => Ok(obj.clone().into_arc()),
                _ => Err(CoreError::QueryValidationError(format!(
                    "Argument '{}' has to be an object type.",
                    name
                ))),
            }
        }

        let inner =
            match model_operation.operation {
                OperationTag::CreateOne => {
                    let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "data")?).split();
                    let mut non_list_args = values.to_prisma_values(arg_object("data", schema_field)?);
                    extend_defaults(&model, &mut non_list_args);

                    let mut non_list_args: PrismaArgs = non_list_args.into();
                    non_list_args.add_datetimes(Arc::clone(&model));

                    let list_args = lists.into_iter().map(|la| la.convert()).collect();
                    let nested_mutactions = build_nested_root(
                        model.name.as_str(),
                        &nested,
                        Arc::clone(&model),
                        model_operation.operation,
                    )?;

                    TopLevelDatabaseMutaction::CreateNode(CreateNode {
                        model: Arc::clone(&model),
                        non_list_args,
                        list_args,
                        nested_mutactions,
                    })
                }

                OperationTag::UpdateOne => {
                    let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "data")?).split();
                    let non_list_args = values.to_prisma_values(arg_object("data", schema_field)?).into();
                    let list_args = lists.into_iter().map(|la| la.convert()).collect();
                    let nested_mutactions = build_nested_root(
                        model.name.as_str(),
                        &nested,
                        Arc::clone(&model),
                        model_operation.operation,
                    )?;

                    let where_ = ValueMap(shift_data(&args, "where")?)
                        .to_node_selector(Arc::clone(&model), arg_object("where", schema_field)?)
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
                    let non_list_args = values.to_prisma_values(arg_object("data", schema_field)?).into();
                    let list_args = lists.into_iter().map(|la| la.convert()).collect();
                    let nested_mutactions = build_nested_root(
                        model.name.as_str(),
                        &nested,
                        Arc::clone(&model),
                        model_operation.operation,
                    )?;

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

                OperationTag::DeleteOne => {
                    let where_ = ValueMap(shift_data(&args, "where")?)
                        .to_node_selector(Arc::clone(&model), arg_object("where", schema_field)?)
                        .map_or(
                            Err(CoreError::QueryValidationError("No `where` on connect".into())),
                            |w| Ok(w),
                        )?;

                    TopLevelDatabaseMutaction::DeleteNode(DeleteNode { where_ })
                }

                OperationTag::DeleteMany => {
                    let query_args = utils::extract_query_args(self.field, Arc::clone(&model))?;
                    let filter = query_args.filter.map(|f| Ok(f)).unwrap_or_else(|| {
                        Err(CoreError::QueryValidationError("Required filters not found!".into()))
                    })?;

                    TopLevelDatabaseMutaction::DeleteNodes(DeleteNodes { model, filter })
                }

                OperationTag::UpsertOne => {
                    let where_ = utils::extract_node_selector(self.field, Arc::clone(&model))?;

                    let create = {
                        let ValueSplit { values, lists, nested } = ValueMap(shift_data(&args, "create")?).split();
                        let mut non_list_args = values.to_prisma_values(arg_object("create", schema_field)?);
                        extend_defaults(&model, &mut non_list_args);

                        let mut non_list_args: PrismaArgs = non_list_args.into();
                        non_list_args.add_datetimes(Arc::clone(&model));

                        let list_args = lists.into_iter().map(|la| la.convert()).collect();
                        let nested_mutactions = build_nested_root(
                            model.name.as_str(),
                            &nested,
                            Arc::clone(&model),
                            model_operation.operation,
                        )?;
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
                        let non_list_args = values.to_prisma_values(arg_object("update", schema_field)?).into();
                        let list_args = lists.into_iter().map(|la| la.convert()).collect();
                        let nested_mutactions = build_nested_root(
                            model.name.as_str(),
                            &nested,
                            Arc::clone(&model),
                            model_operation.operation,
                        )?;
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
        LookAhead::eval(WriteQuery {
            inner,
            name: raw_name,
            field: self.field.clone(),
        })
    }
}

/// A trap-door function that handles `resetData` without doing a whole bunch of other stuff
fn handle_reset(field: &Field, internal_data_model: InternalDataModelRef) -> CoreResult<WriteQuerySet> {
    Ok(WriteQuerySet::Query(WriteQuery {
        inner: TopLevelDatabaseMutaction::ResetData(ResetData { internal_data_model }),
        name: "resetData".into(),
        field: field.clone(),
    }))
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
fn find_field(name: &str, query_schema: QuerySchemaRef) -> CoreResult<FieldRef> {
    match query_schema.find_mutation_field(name) {
        Some(ref field) => Ok(Arc::clone(field)),

        None => Err(CoreError::QueryValidationError(format!(
            "Field not found on type Mutation: {}",
            name
        ))),
    }
}

/// Build nested mutations for a given field/model (called recursively)
pub(crate) fn build_nested_root<'f>(
    name: &'f str,
    args: &'f ValueMap,
    model: ModelRef,
    top_level: OperationTag,
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
