use super::{PrismaRequest, RequestHandler};
use crate::context::PrismaContext;
use crate::schema::Validatable;
use core::{PrismaQuery, PrismaQueryResult, RootQueryBuilder};
use graphql_parser as gql;
use prisma_common::{error::Error, PrismaResult};
use prisma_models::{GraphqlId, PrismaValue, SingleNode};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GraphQlBody {
    query: String,
    operation_name: Option<String>,
    variables: HashMap<String, String>,
}

pub struct GraphQlRequestHandler;

impl RequestHandler for GraphQlRequestHandler {
    type Body = GraphQlBody;

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S, ctx: &PrismaContext) -> serde_json::Value {
        // Handle incoming request and deal with errors properly
        match handle_safely(req.into(), ctx) {
            Ok(val) => val,
            Err(err) => {
                let mut map = serde_json::Map::new();
                map.insert("reason".into(), serde_json::Value::String(err));
                json_envelope("error", map)
            }
        }
    }
}

fn handle_safely(req: PrismaRequest<GraphQlBody>, ctx: &PrismaContext) -> PrismaResult<serde_json::Value> {
    let query_doc = match gql::parse_query(&req.body.query) {
        Ok(doc) => doc,
        Err(e) => return Err(Error::QueryParsingError(format!("{:?}", e))),
    };

    // Let's validate the schema!
    if let Err(_) = ctx.schema.validate(&query_doc) {
        return Err(Error::QueryValidationError(
            "Schema validation failed for unknown reasons".into(),
        ));
    }

    let qb = RootQueryBuilder {
        query: query_doc,
        schema: ctx.schema.clone(),
        operation_name: req.body.operation_name,
    };

    let queries: Vec<PrismaQuery> = qb.build()?;
    let results = dbg!(ctx.query_executor.execute(&queries))?;

    Ok(json_envelope(
        "data",
        results.iter().fold(Ok(serde_json::Map::new()), |acc, result| {
            serialize_tree(&mut acc?, result).map(|_| acc)?
        })?,
    ))
}

/// Recursively serialize query results
fn serialize_tree(
    map: &mut serde_json::map::Map<String, serde_json::Value>,
    result: &PrismaQueryResult,
) -> PrismaResult<()> {
    match result {
        PrismaQueryResult::Single(result) => {
            let json = match result.result {
                None => serde_json::Value::Null,
                Some(single_node) => {
                    let mut map = serialize_single_node(single_node)?;
                    for result in result.nested {
                        serialize_tree(&mut map, &result)?;
                    }
                    serde_json::Value::Object(map)
                }
            };
            map.insert(result.name, json);
        }
        _ => unimplemented!(),
    }

    Ok(())
}

fn serialize_single_node(single_node: SingleNode) -> PrismaResult<serde_json::map::Map<String, serde_json::Value>> {
    let mut serde_map = serde_json::map::Map::new();
    let field_names = single_node.field_names;
    let values = single_node.node.values;

    for (field, value) in field_names.into_iter().zip(values) {
        let key = field.to_string();
        let value = serialize_prisma_value(value)?;
        serde_map.insert(key, value);
    }
    Ok(serde_map)
}

fn serialize_prisma_value(value: PrismaValue) -> PrismaResult<serde_json::Value> {
    Ok(match value {
        PrismaValue::String(x) => serde_json::Value::String(x),
        PrismaValue::Float(x) => serde_json::Value::Number(match serde_json::Number::from_f64(x) {
            Some(num) => num,
            None => return Err(Error::SerialisationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Boolean(x) => serde_json::Value::Bool(x),
        PrismaValue::DateTime(_) => unimplemented!(),
        PrismaValue::Enum(x) => serde_json::Value::String(x),
        PrismaValue::Json(x) => serde_json::from_str(&x)?,
        PrismaValue::Int(x) => serde_json::Value::Number(match serde_json::Number::from_f64(x as f64) {
            Some(num) => num,
            None => return Err(Error::SerialisationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Relation(_) => unimplemented!(),
        PrismaValue::Null => serde_json::Value::Null,
        PrismaValue::Uuid(x) => serde_json::Value::String(x),
        PrismaValue::GraphqlId(x) => serialize_graphql_id(x)?,
    })
}

fn serialize_graphql_id(id: GraphqlId) -> PrismaResult<serde_json::Value> {
    Ok(match id {
        GraphqlId::String(x) => serde_json::Value::String(x),
        GraphqlId::Int(x) => serde_json::Value::Number(match serde_json::Number::from_f64(x as f64) {
            Some(num) => num,
            None => return Err(Error::SerialisationError("`f64` number was invalid".into())),
        }),
        GraphqlId::UUID(x) => serde_json::Value::String(x),
    })
}

/// Create a json envelope
fn json_envelope(id: &str, map: serde_json::Map<String, serde_json::Value>) -> serde_json::Value {
    let mut envelope = serde_json::map::Map::new();
    envelope.insert(id.to_owned(), serde_json::Value::Object(map));
    serde_json::Value::Object(envelope)
}
