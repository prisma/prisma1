use super::{PrismaRequest, RequestHandler};
use crate::context::PrismaContext;
use crate::schema::Validatable;
use core::PrismaQueryResult;
use core::{PrismaQuery, RootQueryBuilder};
use graphql_parser as gql;
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
        let req = req.into();
        let query_doc = gql::parse_query(&req.body.query).unwrap();

        if let Ok(()) = ctx.schema.validate(&query_doc) {
            dbg!(&query_doc);
        }

        let qb = RootQueryBuilder {
            query: query_doc,
            schema: ctx.schema.clone(),
            operation_name: req.body.operation_name,
        };

        let queries: Vec<PrismaQuery> = qb.build().unwrap(); // FIXME: Error handling
        let results = dbg!(ctx.query_executor.execute(&queries)).unwrap();

        /// Recursively serialize query results
        fn serialize(map: &mut serde_json::map::Map<String, serde_json::Value>, result: PrismaQueryResult) {
            match result {
                PrismaQueryResult::Single(result) => {
                    let json = match result.result {
                        None => serde_json::Value::Null,
                        Some(single_node) => {
                            let mut map = serialize_single_node(single_node);
                            for result in result.nested {
                                serialize(&mut map, result);
                            }
                            serde_json::Value::Object(map)
                        }
                    };
                    map.insert(result.name, json);
                }
                _ => unimplemented!(),
            }
        }

        let mut serde_map = serde_json::map::Map::new();
        for result in results {
            serialize(&mut serde_map, result);
        }
        let mut envelope = serde_json::map::Map::new();
        envelope.insert("data".to_owned(), serde_json::Value::Object(serde_map));
        serde_json::Value::Object(envelope)
    }
}

fn serialize_single_node(single_node: SingleNode) -> serde_json::map::Map<String, serde_json::Value> {
    let mut serde_map = serde_json::map::Map::new();
    let field_names = single_node.field_names;
    let values = single_node.node.values;

    for (field, value) in field_names.into_iter().zip(values) {
        let key = field.to_string();
        let value = serialize_prisma_value(value);
        serde_map.insert(key, value);
    }
    serde_map
}

fn serialize_prisma_value(value: PrismaValue) -> serde_json::Value {
    match value {
        PrismaValue::String(x) => serde_json::Value::String(x),
        PrismaValue::Float(x) => {
            let num = serde_json::Number::from_f64(x).unwrap();
            serde_json::Value::Number(num)
        }
        PrismaValue::Boolean(x) => serde_json::Value::Bool(x),
        PrismaValue::DateTime(_) => unimplemented!(),
        PrismaValue::Enum(x) => serde_json::Value::String(x),
        PrismaValue::Json(x) => serde_json::from_str(&x).unwrap(),
        PrismaValue::Int(x) => {
            let num = serde_json::Number::from_f64(x as f64).unwrap();
            serde_json::Value::Number(num)
        }
        PrismaValue::Relation(_) => unimplemented!(),
        PrismaValue::Null => serde_json::Value::Null,
        PrismaValue::Uuid(x) => serde_json::Value::String(x),
        PrismaValue::GraphqlId(x) => serialize_graphql_id(x),
    }
}

fn serialize_graphql_id(id: GraphqlId) -> serde_json::Value {
    match id {
        GraphqlId::String(x) => serde_json::Value::String(x),
        GraphqlId::Int(x) => {
            let num = serde_json::Number::from_f64(x as f64).unwrap();
            serde_json::Value::Number(num)
        }
        GraphqlId::UUID(x) => serde_json::Value::String(x),
    }
}
