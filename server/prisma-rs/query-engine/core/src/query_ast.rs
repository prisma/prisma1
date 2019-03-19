//! Prisma query AST module

use connector::NodeSelector;
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
    // args: QueryArguments,
    // selectedFields: SelectedFields,
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
    // parentField: RelationField,
    // args: QueryArguments,
    // selectedFields: SelectedFields,
    pub nested: Vec<PrismaQuery>,
}

pub struct QueryBuilder {
    pub query: Document,
    pub schema: SchemaRef,
    pub operation_name: Option<String>,
}

enum QueryType {
    Single(ModelRef),
    Multiple(ModelRef),
}

impl QueryBuilder {
    fn build(self) -> PrismaResult<Vec<PrismaQuery>> {
        self.query
            .definitions
            .into_iter()
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
            .collect::<PrismaResult<Vec<Vec<PrismaQuery>>>>()
            .flatten()
    }

    // Q: How do you infer multi or single relation?
    fn build_query(&self, root_fields: &Vec<Selection>) -> PrismaResult<Vec<PrismaQuery>> {
        root_fields
            .iter()
            .map(|item| {
                // First query-level fields map to a model in our schema, either a plural or singular
                match item {
                    Selection::Field(root_field) => {
                        // Find model for field
                        let model = match self.infer_query_type(root_field)? {
                            QueryType::Single(model) => model,
                            QueryType::Multiple(model) => model,
                        };

                        let (_, value) = root_field.arguments.first().expect("no arguments found"); // FIXME: this expects at least one query arg...
                        match value {
                            Value::Object(obj) => {
                                let (field_name, value) = obj.iter().next().expect("object was empty");
                                let field = model.fields().find_from_scalar(field_name).unwrap();
                                let value = Self::value_to_prisma_value(value);
                                let name = root_field.alias.as_ref().unwrap_or(&root_field.name).clone();
                                let selected_fields: Vec<SelectedField> =
                                    Self::to_selected_fields(&root_field.selection_set, Arc::clone(&model));

                                PrismaQuery::RecordQuery(RecordQuery {
                                    name: name,
                                    selector: NodeSelector {
                                        field: field.clone(),
                                        value: value,
                                    },
                                    selected_fields: SelectedFields::new(selected_fields, None),
                                    nested: Self::collect_sub_queries(&root_field.selection_set, Arc::clone(&model)),
                                })
                            }
                            _ => unimplemented!(),
                        }
                    }
                    _ => unimplemented!(),
                }
            })
            .collect();

        unimplemented!()
    }

    /// Finds the model and infers the query type for the given GraphQL field.
    fn infer_query_type(&self, field: &gql::query::Field) -> PrismaResult<QueryType> {
        // Find model for field
        let mut query_type: Option<QueryType> = None;
        for model in self.schema.models() {
            if model.name.to_lowercase() == field.name {
                query_type.replace(QueryType::Single(Arc::clone(&model)));
                break;
            } else if model.name.to_lowercase().to_singular() == field.name {
                query_type.replace(QueryType::Multiple(Arc::clone(&model)));
                break;
            }
        }

        match query_type {
            Some(model_type) => Ok(model_type),
            None => Err(Error::QueryValidationError(format!(
                "Model not found for field {}",
                field.alias.unwrap_or(field.name)
            ))),
        }
    }

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

    fn value_to_prisma_value(val: &Value) -> PrismaValue {
        match val {
            Value::String(s) => PrismaValue::String(s.clone()),
            Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap() as i32),
            _ => unimplemented!(),
        }
    }
}
