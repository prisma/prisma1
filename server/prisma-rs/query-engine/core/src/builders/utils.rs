//! A set of utilities to build (read & write) queries

use super::filters;
use crate::{Builder, BuilderExt, CoreError, CoreResult, ReadQuery};
use connector::{filter::RecordFinder, QueryArguments};
use graphql_parser::query::{Field, Selection, Value};
use prisma_models::{
    Field as ModelField, GraphqlId, InternalDataModelRef, ModelRef, OrderBy, PrismaValue, RelationFieldRef,
    SelectedField, SelectedFields, SelectedRelationField, SelectedScalarField, SortOrder,
};

use std::{collections::BTreeMap, sync::Arc};
use uuid::Uuid;

/// Get record finder from field and model
pub(crate) fn extract_record_finder(field: &Field, model: ModelRef) -> CoreResult<RecordFinder> {
    // FIXME: this expects at least one query arg...
    let (_, value) = field.arguments.first().expect("no arguments found");
    match value {
        Value::Object(obj) => {
            let (field_name, value) = obj.iter().next().expect("object was empty");
            let field = model.fields().find_from_scalar(field_name).unwrap();
            let value = PrismaValue::from_value_broken(value);

            Ok(RecordFinder {
                field: Arc::clone(&field),
                value: value,
            })
        }
        _ => unreachable!(),
    }
}

pub(crate) fn extract_query_args_inner<'a>(
    iter: impl Iterator<Item = (&'a String, &'a Value)>,
    model: ModelRef,
) -> CoreResult<QueryArguments> {
    iter
        .fold(Ok(QueryArguments::default()), |result, (k, v)| {
            if let Ok(res) = result {
                #[cfg_attr(rustfmt, rustfmt_skip)]
                match (k.to_lowercase().as_str(), v) {
                    ("skip", Value::Int(num)) => match num.as_i64() {
                        Some(num) => Ok(QueryArguments { skip: Some(num as u32), ..res }),
                        None => Err(CoreError::QueryValidationError("Invalid number provided".into())),
                    },
                    ("first", Value::Int(num)) => match num.as_i64() {
                        Some(num) => Ok(QueryArguments { first: Some(num as u32), ..res }),
                        None => Err(CoreError::QueryValidationError("Invalid number provided".into())),
                    },
                    ("last", Value::Int(num)) => match num.as_i64() {
                        Some(num) => Ok(QueryArguments { last: Some(num as u32), ..res }),
                        None => Err(CoreError::QueryValidationError("Invalid number provided".into())),
                    },
                    ("after", Value::String(s)) if s.is_uuid() => Ok(QueryArguments { after: Some(GraphqlId::UUID(s.as_uuid())), ..res }),
                    ("after", Value::String(s)) => Ok(QueryArguments { after: Some(s.clone().into()), ..res }),
                    ("after", Value::Int(num)) => match num.as_i64() {
                        Some(num) => Ok(QueryArguments { after: Some((num as usize).into()), ..res }),
                        None => Err(CoreError::QueryValidationError("Invalid number provided".into())),
                    },
                    ("before", Value::String(s)) if s.is_uuid() => Ok(QueryArguments { before: Some(GraphqlId::UUID(s.as_uuid())), ..res }),
                    ("before", Value::String(s)) => Ok(QueryArguments { before: Some(s.clone().into()), ..res }),
                    ("before", Value::Int(num)) => match num.as_i64() {
                        Some(num) => Ok(QueryArguments { after: Some((num as usize).into()), ..res }),
                        None => Err(CoreError::QueryValidationError("Invalid number provided".into())),
                    },
                    ("orderby", Value::Enum(order_arg)) => extract_order_by(res, order_arg, Arc::clone(&model)),
                    ("where", Value::Object(o)) => extract_filter(res, o, Arc::clone(&model)),
                    (name, _) => Err(CoreError::QueryValidationError(format!("Unknown key: `{}`", name))),
                }
            } else {
                result
            }
        })
}

/// Extract query arguments and filters from a field
pub(crate) fn extract_query_args(field: &Field, model: ModelRef) -> CoreResult<QueryArguments> {
    extract_query_args_inner(
        field
            .arguments
            .iter()
            .filter(|(arg, _)| arg.as_str() != "data")
            .map(|(k, v)| (k, v)),
        model,
    )
}

pub(crate) fn extract_order_by(
    aggregator: QueryArguments,
    order_arg: &str,
    model: ModelRef,
) -> CoreResult<QueryArguments> {
    let vec = order_arg.split("_").collect::<Vec<&str>>();
    if vec.len() == 2 {
        model
            .fields()
            .find_from_scalar(vec[0])
            .map(|val| QueryArguments {
                order_by: Some(OrderBy {
                    field: Arc::clone(&val),
                    sort_order: match vec[1] {
                        "ASC" => SortOrder::Ascending,
                        "DESC" => SortOrder::Descending,
                        _ => unreachable!(),
                    },
                }),
                ..aggregator
            })
            .map_err(|_| CoreError::QueryValidationError(format!("Unknown field `{}`", vec[0])))
    } else {
        Err(CoreError::QueryValidationError("...".into()))
    }
}

pub(crate) fn extract_filter(
    aggregator: QueryArguments,
    map: &BTreeMap<String, Value>,
    model: ModelRef,
) -> CoreResult<QueryArguments> {
    let filter = filters::extract_filter(map, model)?;

    Ok(QueryArguments {
        filter: Some(filter),
        ..aggregator
    })
}

/// Get all selected fields from a model
pub(crate) fn collect_selected_fields<I: Into<Option<RelationFieldRef>>>(
    model: ModelRef,
    field: &Field,
    parent: I,
) -> CoreResult<SelectedFields> {
    field
        .selection_set
        .items
        .iter()
        .filter_map(|i| {
            if let Selection::Field(f) = i {
                // We have to make sure the selected field exists in some form.
                let field = model.fields().find_from_all(&f.name);
                match field {
                    Ok(ModelField::Scalar(field)) => Some(Ok(SelectedField::Scalar(SelectedScalarField {
                        field: Arc::clone(&field),
                        implicit: false,
                    }))),
                    // Relation fields are not handled here, but in nested queries
                    Ok(ModelField::Relation(field)) => Some(Ok(SelectedField::Relation(SelectedRelationField {
                        field: Arc::clone(&field),
                        selected_fields: SelectedFields::new(vec![], None),
                    }))),
                    _ => Some(Err(CoreError::QueryValidationError(format!(
                        "Selected field {} not found on model {}",
                        f.name, model.name,
                    )))),
                }
            } else {
                Some(Err(CoreError::UnsupportedFeatureError(
                    "Fragments and inline fragment spreads.".into(),
                )))
            }
        })
        .collect::<CoreResult<Vec<_>>>()
        .map(|sf| SelectedFields::new(sf, parent.into()))
}

pub(crate) fn collect_nested_queries<'field>(
    model: ModelRef,
    ast_field: &'field Field,
    _internal_data_model: InternalDataModelRef,
) -> CoreResult<Vec<Builder<'field>>> {
    ast_field
        .selection_set
        .items
        .iter()
        .filter_map(|i| {
            if let Selection::Field(x) = i {
                let field = &model.fields().find_from_all(&x.name);
                match &field {
                    Ok(ModelField::Scalar(_f)) => None,
                    Ok(ModelField::Relation(f)) => {
                        let model = f.related_model();
                        let parent = Some(Arc::clone(&f));

                        Builder::infer_nested(&model, x, parent).map(|r| Ok(r))
                    }
                    _ => Some(Err(CoreError::QueryValidationError(format!(
                        "Selected field {} not found on model {}",
                        x.name, model.name,
                    )))),
                }
            } else {
                Some(Err(CoreError::UnsupportedFeatureError(
                    "Fragments and inline fragment spreads.".into(),
                )))
            }
        })
        .collect()
}

pub(crate) fn build_nested_queries(builders: Vec<Builder>) -> CoreResult<Vec<ReadQuery>> {
    builders
        .into_iter()
        .map(|b| match b {
            Builder::OneRelation(b) => Ok(ReadQuery::RelatedRecordQuery(b.build()?)),
            Builder::ManyRelation(b) => Ok(ReadQuery::ManyRelatedRecordsQuery(b.build()?)),
            _ => unreachable!(),
        })
        .collect()
}

pub(crate) fn collect_selection_order(field: &Field) -> Vec<String> {
    field
        .selection_set
        .items
        .iter()
        .filter_map(|select| {
            if let Selection::Field(field) = select {
                Some(field.alias.clone().unwrap_or_else(|| field.name.clone()))
            } else {
                None
            }
        })
        .collect()
}

/// A function that derives a field given a field
///
/// This function is used when creating ReadQueries after a WriteQuery has succeeded
pub(crate) fn derive_field(field: &Field, model: ModelRef, id: GraphqlId, mutation_name: &String) -> Field {
    let mut new = field.clone();

    // Set alias to query name and override field name
    new.name = model.name.to_lowercase();
    new.alias = Some(mutation_name.clone());

    // Create a selection set for this ID
    let id_name = model.fields().id().name.clone();
    let mut map = BTreeMap::new();
    map.insert(id_name, id.to_value());

    // Then override the existing arguments
    new.arguments = vec![("where".into(), Value::Object(map))];

    new
}

pub trait UuidString {
    fn is_uuid(&self) -> bool;

    /// This panics if not UUID
    fn as_uuid(&self) -> Uuid;
}

impl UuidString for String {
    fn is_uuid(&self) -> bool {
        Uuid::parse_str(self.as_str()).map(|_| true).unwrap_or(false)
    }

    fn as_uuid(&self) -> Uuid {
        Uuid::parse_str(self.as_str()).unwrap()
    }
}
