//! Query execution builders module

mod filters;
mod inflector;
mod many;
mod many_rel;
mod one_rel;
mod root;
mod single;

pub use many::*;
pub use many_rel::*;
pub use one_rel::*;
pub use root::*;
pub use single::*;

use self::inflector::Inflector;
use crate::{CoreError, CoreResult, ReadQuery};
use ::inflector::Inflector as RustInflector;
use connector::{filter::NodeSelector, QueryArguments};
use graphql_parser::query::{Field, Selection, Value};
use prisma_models::{
    Field as ModelField, GraphqlId, ModelRef, OrderBy, PrismaValue, RelationFieldRef, SchemaRef, SelectedField,
    SelectedFields, SelectedScalarField, SortOrder,
};

use std::{collections::BTreeMap, sync::Arc};
use uuid::Uuid;

/// A common query-builder type
pub enum Builder<'field> {
    Single(SingleBuilder<'field>),
    Many(ManyBuilder<'field>),
    OneRelation(OneRelationBuilder<'field>),
    ManyRelation(ManyRelationBuilder<'field>),
}

impl<'a> Builder<'a> {
    fn new(schema: SchemaRef, root_field: &'a Field) -> CoreResult<Self> {
        // Find model for field - this is a temporary workaround before we have a data model definition (/ schema builder).
        let builder: Option<Builder> = schema
            .models()
            .iter()
            .filter_map(|model| Builder::infer(model, root_field, None))
            .nth(0);

        match builder {
            Some(b) => Ok(b),
            None => Err(CoreError::QueryValidationError(format!(
                "Model not found for field {}",
                root_field.alias.as_ref().unwrap_or(&root_field.name)
            ))),
        }
    }

    /// Infer the type of builder that should be created
    fn infer(model: &ModelRef, field: &'a Field, parent: Option<RelationFieldRef>) -> Option<Builder<'a>> {
        if let Some(ref parent) = parent {
            if parent.is_list {
                Some(Builder::ManyRelation(ManyRelationBuilder::new().setup(
                    Arc::clone(&model),
                    field,
                    Arc::clone(&parent),
                )))
            } else {
                Some(Builder::OneRelation(OneRelationBuilder::new().setup(
                    Arc::clone(&model),
                    field,
                    Arc::clone(&parent),
                )))
            }
        } else {
            let normalized = model.name.to_camel_case();
            if field.name == normalized {
                Some(Builder::Single(SingleBuilder::new().setup(Arc::clone(model), field)))
            } else if Inflector::singularize(&field.name) == normalized {
                Some(Builder::Many(ManyBuilder::new().setup(Arc::clone(model), field)))
            } else {
                None
            }
        }
    }

    fn build(self) -> CoreResult<ReadQuery> {
        match self {
            Builder::Single(b) => Ok(ReadQuery::RecordQuery(b.build()?)),
            Builder::Many(b) => Ok(ReadQuery::ManyRecordsQuery(b.build()?)),
            Builder::OneRelation(b) => Ok(ReadQuery::RelatedRecordQuery(b.build()?)),
            Builder::ManyRelation(b) => Ok(ReadQuery::ManyRelatedRecordsQuery(b.build()?)),
        }
    }
}

/// A trait that describes a query builder
pub trait BuilderExt {
    type Output;

    /// A common cosntructor for all query builders
    fn new() -> Self;

    /// Last step that invokes query building
    fn build(self) -> CoreResult<Self::Output>;

    /// Get node selector from field and model
    fn extract_node_selector(field: &Field, model: ModelRef) -> CoreResult<NodeSelector> {
        // FIXME: this expects at least one query arg...
        let (_, value) = field.arguments.first().expect("no arguments found");
        match value {
            Value::Object(obj) => {
                let (field_name, value) = obj.iter().next().expect("object was empty");
                let field = model.fields().find_from_scalar(field_name).unwrap();
                let value = Self::value_to_prisma_value(value);

                Ok(NodeSelector {
                    field: Arc::clone(&field),
                    value: value,
                })
            }
            _ => unimplemented!(),
        }
    }

    /// Turning a GraphQL value to a PrismaValue
    fn value_to_prisma_value(val: &Value) -> PrismaValue {
        match val {
            Value::String(s) => PrismaValue::String(s.clone()),
            Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap() as i32),
            _ => unimplemented!(),
        }
    }

    fn extract_query_args(field: &Field, model: ModelRef) -> CoreResult<QueryArguments> {
        field
            .arguments
            .iter()
            .fold(Ok(QueryArguments::default()), |result, (k, v)| {
                if let Ok(res) = result {
                    #[cfg_attr(rustfmt, rustfmt_skip)]
                    match (k.as_str(), v) {
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
                        ("orderby", Value::Enum(order_arg)) => Self::extract_order_by(res, order_arg, Arc::clone(&model)),
                        ("where", Value::Object(o)) => Self::extract_filter(res, o, Arc::clone(&model)),
                        (name, _) => Err(CoreError::QueryValidationError(format!("Unknown key: `{}`", name))),
                    }
                } else {
                    result
                }
            })
    }

    fn extract_order_by(aggregator: QueryArguments, order_arg: &str, model: ModelRef) -> CoreResult<QueryArguments> {
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

    fn extract_filter(
        aggregator: QueryArguments,
        map: &BTreeMap<String, Value>,
        model: ModelRef,
    ) -> CoreResult<QueryArguments> {
        unimplemented!()
    }

    /// Get all selected fields from a model
    fn collect_selected_fields<I: Into<Option<RelationFieldRef>>>(
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
                        Ok(ModelField::Relation(_field)) => None,
                        _ => Some(Err(CoreError::QueryValidationError(format!(
                            "Selected field {} not found on model {}",
                            f.name, model.name,
                        )))),
                    }
                } else {
                    // Todo: We only support selecting fields at the moment.
                    unimplemented!()
                }
            })
            .collect::<CoreResult<Vec<_>>>()
            .map(|sf| SelectedFields::new(sf, parent.into()))
    }

    fn collect_nested_queries<'field>(
        model: ModelRef,
        ast_field: &'field Field,
        _schema: SchemaRef,
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

                            Builder::infer(&model, x, parent).map(|r| Ok(r))
                        }
                        _ => Some(Err(CoreError::QueryValidationError(format!(
                            "Selected field {} not found on model {}",
                            x.name, model.name,
                        )))),
                    }
                } else {
                    panic!("We only support selecting fields at the moment!");
                }
            })
            .collect()
    }

    fn build_nested_queries(builders: Vec<Builder>) -> CoreResult<Vec<ReadQuery>> {
        builders
            .into_iter()
            .map(|b| match b {
                Builder::OneRelation(b) => Ok(ReadQuery::RelatedRecordQuery(b.build()?)),
                Builder::ManyRelation(b) => Ok(ReadQuery::ManyRelatedRecordsQuery(b.build()?)),
                _ => unreachable!(),
            })
            .collect()
    }

    fn collect_selection_order(field: &Field) -> Vec<String> {
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
}

trait UuidString {
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
