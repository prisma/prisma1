use super::{PrismaRequest, RequestHandler};
use crate::{context::PrismaContext, error::PrismaError, schema::Validatable, PrismaResult};
use core::{MultiPrismaQueryResult, PrismaQuery, PrismaQueryResult, RootQueryBuilder};
use graphql_parser as gql;
use prisma_models::{GraphqlId, PrismaValue, SingleNode};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

use serde_json::{Map, Number, Value};

type JsonMap = Map<String, Value>;
type JsonVec = Vec<Value>;

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
                map.insert("reason".into(), format!("{}", err).into());
                json_envelope("error", map)
            }
        }
    }
}

fn handle_safely(req: PrismaRequest<GraphQlBody>, ctx: &PrismaContext) -> PrismaResult<Value> {
    let query_doc = match gql::parse_query(&req.body.query) {
        Ok(doc) => doc,
        Err(e) => return Err(PrismaError::QueryParsingError(format!("{:?}", e))),
    };

    // Let's validate the schema!
    if let Err(_) = ctx.schema.validate(&query_doc) {
        return Err(PrismaError::QueryValidationError(
            "Schema validation failed for unknown reasons".into(),
        ));
    }

    let qb = RootQueryBuilder {
        query: query_doc,
        schema: ctx.schema.clone(),
        operation_name: req.body.operation_name,
    };

    let queries: Vec<PrismaQuery> = qb.build()?;
    let results: Vec<PrismaQueryResult> = dbg!(ctx.query_executor.execute(&queries))?
        .into_iter()
        .map(|r| r.filter())
        .collect();

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
        PrismaQueryResult::Single(result) => map.insert(
            result.name.clone(),
            match &result.result {
                None => Value::Null,
                Some(single_node) => {
                    let mut map = serialize_single_node(single_node)?;
                    for result in &result.nested {
                        serialize_tree(&mut map, result)?;
                    }
                    Value::Object(map)
                }
            },
        ),
        PrismaQueryResult::Multi(result) => {
            map.insert(result.name.clone(), Value::Array(serialize_many_nodes(&result)?))
        }
    };

    Ok(())
}

// FIXME: Should not panic!
fn serialize_many_nodes(many_nodes: &MultiPrismaQueryResult) -> PrismaResult<JsonVec> {
    Ok(many_nodes
        .result
        .as_pairs()
        .into_iter()
        .map(|vec| {
            vec.into_iter()
                .fold(Ok(JsonMap::new()), |mut map: PrismaResult<JsonMap>, (name, value)| {
                    map.as_mut().unwrap().insert(name, serialize_prisma_value(&value)?);
                    map
                })
        })
        .map(|map| Value::Object(map.unwrap()))
        .collect())
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
            None => return Err(PrismaError::SerializationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Boolean(x) => Value::Bool(*x),
        PrismaValue::DateTime(_) => unimplemented!(),
        PrismaValue::Enum(x) => Value::String(x.clone()),
        PrismaValue::Json(x) => serde_json::from_str(&x)?,
        PrismaValue::Int(x) => Value::Number(match Number::from_f64(*x as f64) {
            Some(num) => num,
            None => return Err(PrismaError::SerializationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Relation(_) => unimplemented!(),
        PrismaValue::Null => Value::Null,
        PrismaValue::Uuid(x) => Value::String(x.to_hyphenated().to_string()),
        PrismaValue::GraphqlId(x) => serialize_graphql_id(x)?,
        PrismaValue::List(_) => unimplemented!(),
    })
}

fn serialize_graphql_id(id: &GraphqlId) -> PrismaResult<Value> {
    Ok(match id {
        GraphqlId::String(x) => Value::String(x.clone()),
        GraphqlId::Int(x) => Value::Number(match serde_json::Number::from_f64(*x as f64) {
            Some(num) => num,
            None => return Err(PrismaError::SerializationError("`f64` number was invalid".into())),
        }),
        GraphqlId::UUID(x) => Value::String(x.to_hyphenated().to_string()),
    })
}

/// Create a json envelope
fn json_envelope(id: &str, map: serde_json::Map<String, Value>) -> Value {
    let mut envelope = JsonMap::new();
    envelope.insert(id.to_owned(), Value::Object(map));
    Value::Object(envelope)
}

// #[allow(dead_code)]
// fn get_result_name(result: &PrismaQueryResult) -> String {
//     match result {
//         PrismaQueryResult::Single(SinglePrismaQueryResult {
//             name,
//             result: _,
//             nested: _,
//         }) => name.clone(),
//         PrismaQueryResult::Multi(MultiPrismaQueryResult {
//             name,
//             result: _,
//             nested: _,
//         }) => name.clone(),
//     }
// }
