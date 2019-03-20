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

#[derive(Debug)]
enum QueryType {
    Single(ModelRef),
    Multiple(ModelRef),
    Relation(ModelRef),
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
            QueryType::Relation(m) => Arc::clone(m),
        }
    }
}

type BuilderResult<T> = Option<PrismaResult<T>>;

#[derive(Debug)]
struct QueryBuilder<'a> {
    schema: SchemaRef,
    field: &'a gql::query::Field,
    query_type: BuilderResult<QueryType>,
    name: Option<String>,
    selector: BuilderResult<NodeSelector>,
    selected_fields: BuilderResult<SelectedFields>,
    args: BuilderResult<QueryArguments>,
    parent_field: BuilderResult<RelationFieldRef>,
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

    fn nested(schema: SchemaRef, field: &'a gql::query::Field) -> Self {
        unimplemented!()
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

            // let sf = Self::to_selected_fields(&gql_field.selection_set, Arc::clone(&rf.related_model()));

            let selected_fields = self
                .field
                .selection_set
                .items
                .iter()
                .filter_map(|i| {
                    dbg!(&i);
                    if let Selection::Field(f) = i {
                        // We have to make sure the selected field exists in some form.
                        let field = model.fields().find_from_all(&f.name);
                        match field {
                            Ok(ModelField::Scalar(field)) => Some(Ok(SelectedField::Scalar(SelectedScalarField {
                                field: Arc::clone(&field),
                            }))),
                            Ok(ModelField::Relation(field)) => None,
                            _ => Some(Err(Error::QueryValidationError(format!(
                                "Selected field {} not found on model {}",
                                f.name, model.name,
                            )))),
                        }
                    } else {
                        // Todo: We only support selecting fields at the moment.
                        unimplemented!()
                    }
                })
                .collect::<PrismaResult<Vec<SelectedField>>>();

            self.selected_fields = match qt {
                QueryType::Relation(_) => {
                    if let Some(Ok(ref rel)) = self.parent_field {
                        Some(selected_fields.map(|sf| SelectedFields::new(sf, Some(Arc::clone(rel)))))
                    } else {
                        None
                    }
                }
                _ => Some(selected_fields.map(|sf| SelectedFields::new(sf, None))),
            };
        }

        self

        // SelectedField::Relation(SelectedRelationField {
        //                         field: Arc::clone(&field),
        //                         selected_fields: SelectedFields::new(
        //                             Self::to_selected_fields(&f.selection_set, Arc::clone(&model)),
        //                             None,
        //                         ),
        //                     })

        // SelectedFields::new(selected_fields, None)
    }

    // Todo: Maybe we can merge this with the map selected fields somehow, as the code looks fairly similar
    fn collect_nested_queries(mut self) -> Self {
        if let Some(Ok(ref qt)) = self.query_type {
            let model = qt.model();

            let nested_queries: PrismaResult<Vec<QueryBuilder>> = self
                .field
                .selection_set
                .items
                .iter()
                .filter_map(|i| {
                    if let Selection::Field(f) = i {
                        let field = model.fields().find_from_all(&f.name);
                        match field {
                            Ok(ModelField::Scalar(field)) => None,
                            Ok(ModelField::Relation(field)) => {
                                // Todo: How to handle relations?
                                // The QB needs to know that it's a relation, needs to find the related model, etc.
                                let qb = QueryBuilder::new(Arc::clone(&self.schema), f)
                                    .infer_query_type()
                                    .process_arguments()
                                    .map_selected_scalar_fields()
                                    .collect_nested_queries();

                                Some(Ok(qb))
                            }
                            _ => Some(Err(Error::QueryValidationError(format!(
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
    fn get(self) -> PrismaResult<PrismaQuery> {
        dbg!(&self);
        let name = self.field.alias.as_ref().unwrap_or(&self.field.name).clone();
        let selected_fields = self.selected_fields.unwrap_or(Err(Error::QueryValidationError(
            "Selected fields required but not found".into(),
        )))?;

        let nested_queries = self
            .nested
            .unwrap_or(Err(Error::QueryValidationError(
                "Required nested queries not found".into(),
            )))?
            .into_iter()
            .map(|qb| qb.get())
            .collect::<PrismaResult<Vec<PrismaQuery>>>()?;

        match self.query_type {
            Some(qt) => match qt? {
                // todo: more smaller functions
                QueryType::Single(model) => {
                    let selector = self.selector.unwrap_or(Err(Error::QueryValidationError(
                        "Required node selector not found".into(),
                    )))?;

                    Ok(PrismaQuery::RecordQuery(RecordQuery {
                        name: name,
                        selector: selector,
                        selected_fields: selected_fields,
                        nested: nested_queries,
                    }))
                }
                QueryType::Multiple(model) => unimplemented!(),
                QueryType::Relation(model) => {
                    let parent_field = self.parent_field.unwrap_or(Err(Error::QueryValidationError(
                        "Required parent field not found".into(),
                    )))?;

                    Ok(PrismaQuery::RelatedRecordQuery(RelatedRecordQuery {
                        name: name,
                        parent_field: parent_field,
                        selected_fields: selected_fields,
                        nested: nested_queries,
                    }))
                }
            },
            None => unimplemented!(),
        }
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
                        .map_selected_scalar_fields()
                        .collect_nested_queries()
                        .get(), // Q: Since we never really give any args, and we always have to call these fns, we should just to it internally and call .get
                    _ => unimplemented!(),
                }
            })
            .collect()
    }
}
