//! Prisma query AST module

use crate::{CoreError, CoreResult};
use connector::{filter::NodeSelector, QueryArguments};
use graphql_parser::{self as gql, query::*};
use inflector::Inflector;
use prisma_models::{Field as ModelField, *};
use std::sync::Arc;

#[derive(Debug, Clone)]
pub enum PrismaQuery {
    RecordQuery(RecordQuery),
    MultiRecordQuery(MultiRecordQuery),
    RelatedRecordQuery(RelatedRecordQuery),
    MultiRelatedRecordQuery(MultiRelatedRecordQuery),
}

#[derive(Debug, Clone)]
pub struct RecordQuery {
    pub name: String,
    pub selector: NodeSelector,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug, Clone)]
pub struct MultiRecordQuery {
    pub name: String,
    pub model: ModelRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug, Clone)]
pub struct RelatedRecordQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug, Clone)]
pub struct MultiRelatedRecordQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    pub args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct RootQueryBuilder {
    pub query: Document,
    pub schema: SchemaRef,
    pub operation_name: Option<String>,
}

#[derive(Debug)]
enum QueryType {
    Single(ModelRef),
    Multiple(ModelRef),
    OneRelation(ModelRef),
    ManyRelation(ModelRef),
}

impl QueryType {
    /// Infers the query type from the field name
    fn infer_root(model: &ModelRef, field: &gql::query::Field) -> Option<Self> {
        if model.name.to_camel_case().to_singular() == field.name {
            Some(QueryType::Single(Arc::clone(&model)))
        } else if model.name.to_camel_case().to_plural() == field.name {
            Some(QueryType::Multiple(Arc::clone(&model)))
        } else {
            None
        }
    }

    fn model(&self) -> ModelRef {
        match self {
            QueryType::Single(m) => Arc::clone(m),
            QueryType::Multiple(m) => Arc::clone(m),
            QueryType::OneRelation(m) => Arc::clone(m),
            QueryType::ManyRelation(m) => Arc::clone(m),
        }
    }
}

type BuilderResult<T> = Option<CoreResult<T>>;

#[derive(Debug)]
struct QueryBuilder<'a> {
    schema: SchemaRef,
    field: &'a gql::query::Field,
    query_type: BuilderResult<QueryType>,
    name: Option<String>,
    selector: BuilderResult<NodeSelector>,
    selected_fields: BuilderResult<SelectedFields>,
    args: BuilderResult<QueryArguments>,
    parent_field: Option<RelationFieldRef>,
    nested: BuilderResult<Vec<QueryBuilder<'a>>>,
}

impl<'a> QueryBuilder<'a> {
    fn new(schema: SchemaRef, field: &'a gql::query::Field) -> Self {
        Self {
            schema,
            field,
            query_type: None,
            name: None,
            selector: None,
            selected_fields: None,
            args: None,
            parent_field: None,
            nested: None,
        }
    }

    /// Finds the model and infers the query type for the given GraphQL field.
    fn infer_query_type(mut self, parent: Option<RelationFieldRef>) -> Self {
        self.parent_field = parent;

        self.query_type = if let Some(ref parent) = &self.parent_field {
            if parent.is_list {
                Some(Ok(QueryType::ManyRelation(parent.related_model())))
            } else {
                Some(Ok(QueryType::OneRelation(parent.related_model())))
            }
        } else {
            // Find model for field
            let qt: Option<QueryType> = self
                .schema
                .models()
                .iter()
                .filter_map(|model| QueryType::infer_root(model, self.field))
                .nth(0);

            Some(match qt {
                Some(model_type) => Ok(model_type),
                None => Err(CoreError::QueryValidationError(format!(
                    "Model not found for field {}",
                    self.field.alias.as_ref().unwrap_or(&self.field.name)
                ))),
            })
        };

        self
    }

    fn process_arguments(mut self) -> Self {
        match self.query_type {
            Some(Ok(QueryType::Single(ref m))) => self.selector = Some(self.extract_node_selector(Arc::clone(m))),
            Some(Ok(QueryType::Multiple(ref m))) => self.args = Some(self.extract_query_args(Arc::clone(m))),
            Some(Ok(QueryType::OneRelation(ref m))) => self.args = Some(self.extract_query_args(Arc::clone(m))),
            Some(Ok(QueryType::ManyRelation(ref m))) => self.args = Some(self.extract_query_args(Arc::clone(m))),
            _ => {
                //FIXME: This is really not ideal, where do we store the error in this case?
                // This, and many other places, actually point to a separated query builder for many and single
                let err = Err(CoreError::QueryValidationError("".to_string()));
                self.args = Some(err);
            }
        };

        self
    }

    fn extract_node_selector(&self, model: ModelRef) -> CoreResult<NodeSelector> {
        let (_, value) = self.field.arguments.first().expect("no arguments found"); // FIXME: this expects at least one query arg...
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

    fn extract_query_args(&self, model: ModelRef) -> CoreResult<QueryArguments> {
        self.field
            .arguments
            .iter()
            .fold(Ok(QueryArguments::default()), |result, (k, v)| {
                if let Ok(res) = result {
                    #[cfg_attr(rustfmt, rustfmt_skip)]
                    match (k.as_str(), v) {
                        ("skip", Value::Int(num)) => match num.as_i64() {
                            Some(num) => Ok(QueryArguments { skip: Some(num as u32), ..res }),
                            None => Err(CoreError::QueryValidationError("Invalid number povided".into())),
                        },
                        ("first", Value::Int(num)) => match num.as_i64() {
                            Some(num) => Ok(QueryArguments { first: Some(num as u32), ..res }),
                            None => Err(CoreError::QueryValidationError("Invalid number povided".into())),
                        },
                        ("last", Value::Int(num)) => match num.as_i64() {
                            Some(num) => Ok(QueryArguments { first: Some(num as u32), ..res }),
                            None => Err(CoreError::QueryValidationError("Invalid number povided".into())),
                        },
                        //("after", Value::String(s)) if s.is_uuid() => Ok(QueryArguments { after: Some(UuidString(s.clone()).into()), ..res }),
                        ("after", Value::String(s)) => Ok(QueryArguments { after: Some(s.clone().into()), ..res }),
                        ("after", Value::Int(num)) => match num.as_i64() {
                            Some(num) => Ok(QueryArguments { first: Some(num as u32), ..res }),
                            None => Err(CoreError::QueryValidationError("Invalid number povided".into())),
                        },
                        //("before", Value::String(s)) if s.is_uuid() => Ok(QueryArguments { before: Some(UuidString(s.clone()).into()), ..res }),
                        ("before", Value::String(s)) => Ok(QueryArguments { before: Some(s.clone().into()), ..res }),
                        ("before", Value::Int(num)) => match num.as_i64() {
                            Some(num) => Ok(QueryArguments { first: Some(num as u32), ..res }),
                            None => Err(CoreError::QueryValidationError("Invalid number povided".into())),
                        },
                        ("orderby", Value::Enum(name)) => {
                            let vec = name.split("_").collect::<Vec<&str>>();
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
                                        ..res
                                    })
                                    .map_err(|_| CoreError::QueryValidationError(format!("Unknown field `{}`", vec[0])))
                            } else {
                                Err(CoreError::QueryValidationError("...".into()))
                            }
                        }
                        ("where", _) => panic!("lolnope"),
                        (name, _) => Err(CoreError::QueryValidationError(format!("Unknown key: `{}`", name))),
                    }
                } else {
                    result
                }
            })
    }

    // Todo: From trait somewhere?
    fn value_to_prisma_value(val: &Value) -> PrismaValue {
        match val {
            Value::String(s) => PrismaValue::String(s.clone()),
            Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap() as i32),
            _ => unimplemented!(),
        }
    }

    fn map_selected_scalar_fields(mut self) -> Self {
        if let Some(Ok(ref qt)) = self.query_type {
            let model = qt.model();
            let selected_fields = self
                .field
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
                .collect::<CoreResult<Vec<SelectedField>>>();

            self.selected_fields = match qt {
                QueryType::ManyRelation(_) | QueryType::OneRelation(_) => {
                    if let Some(ref rel) = self.parent_field {
                        Some(selected_fields.map(|sf| SelectedFields::new(sf, Some(Arc::clone(rel)))))
                    } else {
                        None
                    }
                }
                _ => Some(selected_fields.map(|sf| SelectedFields::new(sf, None))),
            };
        }

        self
    }

    // Todo: Maybe we can merge this with the map selected fields somehow, as the code looks fairly similar
    fn collect_nested_queries(mut self) -> Self {
        if let Some(Ok(ref qt)) = self.query_type {
            let model = qt.model();

            let nested_queries: CoreResult<Vec<QueryBuilder>> = self
                .field
                .selection_set
                .items
                .iter()
                .filter_map(|i| {
                    if let Selection::Field(f) = i {
                        let field = model.fields().find_from_all(&f.name);
                        match field {
                            Ok(ModelField::Scalar(_field)) => None,
                            Ok(ModelField::Relation(field)) => {
                                // Todo: How to handle relations?
                                // The QB needs to know that it's a relation, needs to find the related model, etc.
                                let qb = QueryBuilder::new(Arc::clone(&self.schema), f)
                                    .infer_query_type(Some(Arc::clone(&field)))
                                    .process_arguments()
                                    .map_selected_scalar_fields()
                                    .collect_nested_queries();

                                Some(Ok(qb))
                            }
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
                .collect();

            self.nested = Some(nested_queries);
        }

        self
    }

    // Q: Wouldn't it make more sense to just call that one from the outside and not the other ones?
    fn get(self) -> CoreResult<PrismaQuery> {
        dbg!(&self);
        let name = self.field.alias.as_ref().unwrap_or(&self.field.name).clone();
        let selected_fields = self.selected_fields.unwrap_or(Err(CoreError::QueryValidationError(
            "Selected fields required but not found".into(),
        )))?;

        // todo inject id field

        let nested_queries = self
            .nested
            .unwrap_or(Err(CoreError::QueryValidationError(
                "Required nested queries not found".into(),
            )))?
            .into_iter()
            .map(|qb| qb.get())
            .collect::<CoreResult<Vec<PrismaQuery>>>()?;

        // todo this needs some DRYing
        match self.query_type {
            Some(qt) => match qt? {
                // todo: more smaller functions
                QueryType::Single(_model) => {
                    let selector = self.selector.unwrap_or(Err(CoreError::QueryValidationError(
                        "Required node selector not found".into(),
                    )))?;

                    Ok(PrismaQuery::RecordQuery(RecordQuery {
                        name: name,
                        selector: selector,
                        selected_fields: selected_fields,
                        nested: nested_queries,
                    }))
                }
                QueryType::Multiple(model) => {
                    let args = self.args.unwrap_or(Err(CoreError::QueryValidationError(
                        "Required query args not found".into(),
                    )))?;

                    Ok(PrismaQuery::MultiRecordQuery(MultiRecordQuery {
                        name,
                        args,
                        model,
                        selected_fields,
                        nested: nested_queries,
                    }))
                }
                QueryType::OneRelation(_model) => {
                    let parent_field = self
                        .parent_field
                        .map(|i| Ok(i)) // FIXME: 🤮 This is bad
                        .unwrap_or(Err(CoreError::QueryValidationError(
                            "Required parent field not found".into(),
                        )))?;

                    let args = self.args.unwrap_or(Err(CoreError::QueryValidationError(
                        "Required query args not found".into(),
                    )))?;

                    Ok(PrismaQuery::RelatedRecordQuery(RelatedRecordQuery {
                        name: name,
                        parent_field: parent_field,
                        selected_fields: selected_fields,
                        args: args,
                        nested: nested_queries,
                    }))
                }
                QueryType::ManyRelation(_model) => {
                    let parent_field = self
                        .parent_field
                        .map(|i| Ok(i)) // FIXME: 🤮 This is bad
                        .unwrap_or(Err(CoreError::QueryValidationError(
                            "Required parent field not found".into(),
                        )))?;

                    let args = self.args.unwrap_or(Err(CoreError::QueryValidationError(
                        "Required query args not found".into(),
                    )))?;

                    Ok(PrismaQuery::MultiRelatedRecordQuery(MultiRelatedRecordQuery {
                        name: name,
                        parent_field: parent_field,
                        selected_fields: selected_fields,
                        args: args,
                        nested: nested_queries,
                    }))
                }
            },
            None => Err(CoreError::QueryValidationError("Unknown query type".into())),
        }
    }
}

impl RootQueryBuilder {
    // FIXME: Find op name and only execute op!
    pub fn build(self) -> CoreResult<Vec<PrismaQuery>> {
        self.query
            .definitions
            .iter()
            .map(|d| match d {
                // Query without the explicit "query" before the selection set
                Definition::Operation(OperationDefinition::SelectionSet(SelectionSet { span: _, items })) => {
                    self.build_query(&items)
                }

                // Regular query
                Definition::Operation(OperationDefinition::Query(Query {
                    position: _,
                    name: _,
                    variable_definitions: _,
                    directives: _,
                    selection_set,
                })) => self.build_query(&selection_set.items),
                _ => unimplemented!(),
            })
            .collect::<CoreResult<Vec<Vec<PrismaQuery>>>>() // Collect all the "query trees"
            .map(|v| v.into_iter().flatten().collect())
    }

    fn build_query(&self, root_fields: &Vec<Selection>) -> CoreResult<Vec<PrismaQuery>> {
        root_fields
            .iter()
            .map(|item| {
                // First query-level fields map to a model in our schema, either a plural or singular
                match item {
                    Selection::Field(root_field) => QueryBuilder::new(Arc::clone(&self.schema), root_field)
                        .infer_query_type(None)
                        .process_arguments()
                        .map_selected_scalar_fields()
                        .collect_nested_queries()
                        .get(), // Q: Since we never really give any args, and we always have to call these fns, we should just to it internally and call .get
                    _ => unimplemented!(),
                }
            })
            .collect()
    }
}

trait UuidCheck {
    fn is_uuid(&self) -> bool;
}

impl UuidCheck for String {
    fn is_uuid(&self) -> bool {
        false
    }
}
