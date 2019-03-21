use super::{PrismaRequest, RequestHandler};
use crate::context::PrismaContext;
use crate::schema::Validatable;
use core::{PrismaQuery, PrismaQueryResult, RootQueryBuilder};
use graphql_parser as gql;
use prisma_common::{error::Error, PrismaResult};
use prisma_models::{GraphqlId, PrismaValue, SingleNode};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::error::Error as _;

use serde_json::{Map, Number, Value};

type JsonMap = Map<String, Value>;

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GraphQlBody {
    query: String,
    operation_name: Option<String>,
    variables: HashMap<String, String>,
}

pub struct GraphQlRequestHandler;

#[allow(unused_variables)]
impl RequestHandler for GraphQlRequestHandler {
    type Body = GraphQlBody;

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S, ctx: &PrismaContext) -> Value {
        // Handle incoming request and deal with errors properly
        match handle_safely(req.into(), ctx) {
            Ok(val) => val,
            Err(err) => {
                let mut map = Map::new();
                map.insert("reason".into(), Value::String(err.description().into()));
                json_envelope("error", map)
            }
        }
    }
}

fn handle_safely(req: PrismaRequest<GraphQlBody>, ctx: &PrismaContext) -> PrismaResult<Value> {
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
        results
            .iter()
            .fold(Ok(Map::new()), |acc: PrismaResult<JsonMap>, result| {
                acc.map(|mut a| serialize_tree(&mut a, result).map(|_| a))?
            })?,
    ))
}

/// Recursively serialize query results
fn serialize_tree(map: &mut JsonMap, result: &PrismaQueryResult) -> PrismaResult<()> {
    match result {
        PrismaQueryResult::Single(result) => {
            let json = match &result.result {
                None => Value::Null,
                Some(single_node) => {
                    let mut map = serialize_single_node(single_node)?;
                    for result in &result.nested {
                        serialize_tree(&mut map, &result)?;
                    }
                    Value::Object(map)
                }
            };
            map.insert(result.name.clone(), json);
        }
        _ => unimplemented!(),
    }

    Ok(())
}

fn serialize_single_node(single_node: &SingleNode) -> PrismaResult<JsonMap> {
    let mut serde_map = Map::new();
    let field_names = &single_node.field_names;
    let values = &single_node.node.values;

    for (field, value) in field_names.into_iter().zip(values) {
        let key = field.to_string();
        let value = serialize_prisma_value(value)?;
        serde_map.insert(key, value);
    }
    Ok(serde_map)
}

fn serialize_prisma_value(value: &PrismaValue) -> PrismaResult<Value> {
    Ok(match value {
        PrismaValue::String(x) => Value::String(x.clone()),
        PrismaValue::Float(x) => Value::Number(match Number::from_f64(*x) {
            Some(num) => num,
            None => return Err(Error::SerialisationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Boolean(x) => Value::Bool(*x),
        PrismaValue::DateTime(_) => unimplemented!(),
        PrismaValue::Enum(x) => Value::String(x.clone()),
        PrismaValue::Json(x) => serde_json::from_str(&x)?,
        PrismaValue::Int(x) => Value::Number(match Number::from_f64(*x as f64) {
            Some(num) => num,
            None => return Err(Error::SerialisationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Relation(_) => unimplemented!(),
        PrismaValue::Null => Value::Null,
        PrismaValue::Uuid(x) => Value::String(x.clone()),
        PrismaValue::GraphqlId(x) => serialize_graphql_id(x)?,
    })
}

fn serialize_graphql_id(id: &GraphqlId) -> PrismaResult<Value> {
    Ok(match id {
        GraphqlId::String(x) => Value::String(x.clone()),
        GraphqlId::Int(x) => Value::Number(match serde_json::Number::from_f64(*x as f64) {
            Some(num) => num,
            None => return Err(Error::SerialisationError("`f64` number was invalid".into())),
        }),
        GraphqlId::UUID(x) => Value::String(x.clone()),
    })
}

/// Create a json envelope
fn json_envelope(id: &str, map: serde_json::Map<String, Value>) -> Value {
    let mut envelope = JsonMap::new();
    envelope.insert(id.to_owned(), Value::Object(map));
    Value::Object(envelope)
}
