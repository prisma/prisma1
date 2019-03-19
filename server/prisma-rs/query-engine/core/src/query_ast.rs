//! Prisma query AST module

use connector::NodeSelector;
use connector::QueryArguments;
use graphql_parser::{self as gql, query::*};
use inflector::Inflector;
use prisma_common::{error::Error, PrismaResult};
use prisma_models::{Field as ModelField, *};
use std::sync::Arc;

#[derive(Debug)]
pub enum PrismaQuery {
    RecordQuery(RecordQuery),
    MultiRecordQuery(MultiRecordQuery),
    RelatedRecordQuery(RelatedRecordQuery),
    MultiRelatedRecordQuery(MultiRelatedRecordQuery),
}

#[derive(Debug)]
pub struct RecordQuery {
    pub name: String,
    pub selector: NodeSelector,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug)]
pub struct MultiRecordQuery {
    model: Model,
    args: QueryArguments,
    selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug)]
pub struct RelatedRecordQuery {
    pub name: String,
    pub parent_field: RelationFieldRef,
    // args: QueryArguments,
    pub selected_fields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

#[derive(Debug)]
pub struct MultiRelatedRecordQuery {
    parentField: RelationFieldRef,
    args: QueryArguments,
    selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct RootQueryBuilder {
    pub query: Document,
    pub schema: SchemaRef,
    pub operation_name: Option<String>,
}

enum QueryType {
    Single(ModelRef),
    Multiple(ModelRef),
}

impl QueryType {
    fn lowercase(model: &ModelRef, field: &gql::query::Field) -> Option<Self> {
        if model.name.to_lowercase() == field.name {
            Some(QueryType::Single(Arc::clone(&model)))
        } else {
            None
        }
    }

    fn singular(model: &ModelRef, field: &gql::query::Field) -> Option<Self> {
        if model.name.to_lowercase().to_singular() == field.name {
            Some(QueryType::Multiple(Arc::clone(&model)))
        } else {
            None
        }
    }

    fn model(&self) -> ModelRef {
        match self {
            QueryType::Single(m) => Arc::clone(m),
            QueryType::Multiple(m) => Arc::clone(m),
        }
    }
}

type BuilderResult<T> = Option<PrismaResult<T>>;

struct QueryBuilder<'a> {
    schema: SchemaRef,
    field: &'a gql::query::Field,
    query_type: BuilderResult<QueryType>,
    name: Option<String>,
    selector: BuilderResult<NodeSelector>,
    selected_fields: Option<SelectedFields>,
    args: BuilderResult<QueryArguments>,
    parentField: Option<RelationFieldRef>,
    nested: Vec<QueryBuilder<'a>>,
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
            parentField: None,
            nested: vec![],
        }
    }

    /// Finds the model and infers the query type for the given GraphQL field.
    fn infer_query_type(mut self) -> Self {
        // Find model for field
        let qt: Option<QueryType> = self
            .schema
            .models()
            .iter()
            .filter_map(|model| QueryType::lowercase(model, self.field).or(QueryType::singular(model, self.field)))
            .nth(0);

        self.query_type = Some(match qt {
            Some(model_type) => Ok(model_type),
            None => Err(Error::QueryValidationError(format!(
                "Model not found for field {}",
                self.field.alias.as_ref().unwrap_or(&self.field.name)
            ))),
        });

        self
    }

    fn process_arguments(mut self) -> Self {
        match self.query_type {
            Some(Ok(QueryType::Single(ref m))) => self.selector = Some(self.extract_node_selector(Arc::clone(m))),
            Some(Ok(QueryType::Multiple(ref m))) => self.args = Some(self.extract_query_args(Arc::clone(m))),
            _ => {
                //FIXME: This is really not ideal, where do we store the error in this case?
                // This, and many other places, actually point to a separated query builder for many and single
                let err = Err(Error::QueryValidationError("".to_string()));
                self.args = Some(err);
            }
        };

        self
    }

    fn extract_node_selector(&self, model: ModelRef) -> PrismaResult<NodeSelector> {
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

    fn extract_query_args(&self, model: ModelRef) -> PrismaResult<QueryArguments> {
        unimplemented!()
    }

    // Q: Wouldn't it make more sense to just call that one from the outside?
    fn get(self) -> PrismaResult<PrismaQuery> {
        let name = self.field.alias.as_ref().unwrap_or(&self.field.name).clone();

        match self.query_type {
            Some(qt) => match qt? {
                // todo: more smaller functions
                QueryType::Single(model) => {
                    let selector = self.selector.unwrap_or(Err(Error::QueryValidationError(
                        "Required node selector not found".into(),
                    )))?;

                    let selected_fields: Vec<SelectedField> =
                        Self::to_selected_fields(&self.field.selection_set, Arc::clone(&model));

                    Ok(PrismaQuery::RecordQuery(RecordQuery {
                        name: name,
                        selector: selector,
                        selected_fields: SelectedFields::new(selected_fields, None),
                        nested: Self::collect_sub_queries(&self.field.selection_set, Arc::clone(&model)),
                    }))
                }
                QueryType::Multiple(model) => unimplemented!(),
            },
            None => unimplemented!(),
        }
    }

    // Todo: From trait somewhere?
    fn value_to_prisma_value(val: &Value) -> PrismaValue {
        match val {
            Value::String(s) => PrismaValue::String(s.clone()),
            Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap() as i32),
            _ => unimplemented!(),
        }
    }

    // todo refactor
    fn to_selected_fields(selection_set: &SelectionSet, model: ModelRef) -> Vec<SelectedField> {
        selection_set
            .items
            .iter()
            .map(|i| {
                if let Selection::Field(f) = i {
                    let field = model.fields().find_from_all(&f.name).unwrap();
                    match field {
                        ModelField::Scalar(field) => SelectedField::Scalar(SelectedScalarField {
                            field: Arc::clone(&field),
                        }),
                        ModelField::Relation(field) => SelectedField::Relation(SelectedRelationField {
                            field: Arc::clone(&field),
                            selected_fields: SelectedFields::new(
                                Self::to_selected_fields(&f.selection_set, Arc::clone(&model)),
                                None,
                            ),
                        }),
                    }
                } else {
                    unreachable!()
                }
            })
            .collect()
    }

    fn collect_sub_queries(selection_set: &SelectionSet, model: ModelRef) -> Vec<PrismaQuery> {
        let queries = selection_set
            .items
            .iter()
            .flat_map(|item| match item {
                Selection::Field(gql_field) => {
                    let field = model
                        .fields()
                        .find_from_all(&gql_field.name)
                        .expect("did not find field");

                    match field {
                        ModelField::Relation(rf) => {
                            let sf =
                                Self::to_selected_fields(&gql_field.selection_set, Arc::clone(&rf.related_model()));
                            Some(PrismaQuery::RelatedRecordQuery(RelatedRecordQuery {
                                name: gql_field.name.clone(),
                                parent_field: Arc::clone(&rf),
                                selected_fields: SelectedFields::new(sf, None),
                                nested: vec![],
                            }))
                        }
                        ModelField::Scalar(_) => None,
                    }
                }
                _ => unimplemented!(),
            })
            .collect();
        queries
    }
}

impl RootQueryBuilder {
    // FIXME: Find op name and only execute op!
    pub fn build(self) -> PrismaResult<Vec<PrismaQuery>> {
        self.query
            .definitions
            .iter()
            .map(|d| match d {
                // Query without the explicit "query" before the selection set
                Definition::Operation(OperationDefinition::SelectionSet(SelectionSet { span, items })) => {
                    self.build_query(&items)
                }

                // Regular query
                Definition::Operation(OperationDefinition::Query(Query {
                    position,
                    name,
                    variable_definitions,
                    directives,
                    selection_set,
                })) => self.build_query(&selection_set.items),
                _ => unimplemented!(),
            })
            .collect::<PrismaResult<Vec<Vec<PrismaQuery>>>>() // Collect all the "query trees"
            .map(|v| v.into_iter().flatten().collect())
    }

    fn build_query(&self, root_fields: &Vec<Selection>) -> PrismaResult<Vec<PrismaQuery>> {
        root_fields
            .iter()
            .map(|item| {
                // First query-level fields map to a model in our schema, either a plural or singular
                match item {
                    Selection::Field(root_field) => QueryBuilder::new(Arc::clone(&self.schema), root_field)
                        .infer_query_type()
                        .process_arguments()
                        .get(),
                    _ => unimplemented!(),
                }
            })
            .collect()
    }
}
