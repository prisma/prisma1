//! Prisma query AST module

use connector::NodeSelector;
use graphql_parser::{self as gql, query::*};
use prisma_models::{Model, PrismaValue, SchemaRef, SelectedFields};
use std::convert::From;

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
    // parentField: RelationField,
    // args: QueryArguments,
    // selectedFields: SelectedFields,
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

impl From<QueryBuilder> for Vec<PrismaQuery> {
    fn from(qb: QueryBuilder) -> Self {
        qb.query
            .definitions
            .iter()
            .flat_map(|d| match d {
                Definition::Operation(OperationDefinition::SelectionSet(SelectionSet { span, items })) => {
                    items
                        .iter()
                        .map(|item| {
                            // Top level field -> Model in our schema
                            match item {
                                Selection::Field(outer_field) => {
                                    // Find model for field
                                    let model = qb
                                        .schema
                                        .models()
                                        .iter()
                                        .find(|model| model.name.to_lowercase() == outer_field.name)
                                        .cloned()
                                        .unwrap();

                                    let (name, value) = outer_field.arguments.first().unwrap();
                                    match value {
                                        Value::Object(obj) => {
                                            let (field_name, value) = obj.iter().next().unwrap();
                                            let field = model.fields().find_from_scalar(field_name).unwrap();
                                            let value = value_to_prisma_value(value);

                                            PrismaQuery::RecordQuery(RecordQuery {
                                                name: outer_field.name.clone(),
                                                selector: NodeSelector {
                                                    field: field.clone(),
                                                    value: value,
                                                },
                                                selected_fields: SelectedFields::all_scalar_fields(model),
                                                nested: vec![],
                                            })
                                        }
                                        _ => unimplemented!(),
                                    }
                                }
                                _ => unimplemented!(),
                            }
                        })
                        .collect::<Vec<PrismaQuery>>()
                }
                Definition::Operation(OperationDefinition::Query(Query {
                    position,
                    name,
                    variable_definitions,
                    directives,
                    selection_set,
                })) => unimplemented!(),
                _ => unimplemented!(),
            })
            .collect::<Vec<PrismaQuery>>()
    }
}

fn value_to_prisma_value(val: &Value) -> PrismaValue {
    match val {
        Value::String(s) => PrismaValue::String(s.clone()),
        Value::Int(i) => PrismaValue::Int(i.as_i64().unwrap() as i32),
        _ => unimplemented!(),
    }
}
