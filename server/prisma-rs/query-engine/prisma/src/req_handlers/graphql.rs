use super::{PrismaRequest, RequestHandler};
use crate::context::PrismaContext;
use crate::schema::Validatable;
use core::{PrismaQuery, QueryBuilder};
use graphql_parser as gql;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use prisma_models::{SingleNode, PrismaValue, GraphqlId};

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

        let qb = QueryBuilder {
            query: query_doc,
            schema: ctx.schema.clone(),
            operation_name: req.body.operation_name,
        };

        let queries: Vec<PrismaQuery> = qb.into();

        let result = dbg!(ctx.query_executor.execute(queries)).unwrap();
        
        match result {
            None => serde_json::Value::Null,
            Some(single_node) => serialize_single_node(single_node),
        }
    }

}

fn serialize_single_node(single_node: SingleNode) -> serde_json::Value {
    let mut fields = serde_json::map::Map::new();
    for (i, field) in single_node.field_names.into_iter().enumerate() {
        let key = field.to_string();
        let prisma_value = single_node.node.values[i].clone();
        let value = serialize_prisma_value(prisma_value);
        fields.insert(field.to_string(), value);
    }
    serde_json::Value::Object(fields)
}

fn serialize_prisma_value(value: PrismaValue) -> serde_json::Value {
    match value {
        PrismaValue::String(x) => serde_json::Value::String(x),
        PrismaValue::Float(x) => {
            let num = serde_json::Number::from_f64(x).unwrap();
            serde_json::Value::Number(num)
        },
        PrismaValue::Boolean(x) => serde_json::Value::Bool(x),
        PrismaValue::DateTime(x) => unimplemented!(),
        PrismaValue::Enum(x) => serde_json::Value::String(x),
        PrismaValue::Json(x) => serde_json::from_str(&x).unwrap(),
        PrismaValue::Int(x) => {
            let num = serde_json::Number::from_f64(x as f64).unwrap();
            serde_json::Value::Number(num)
        },
        PrismaValue::Relation(x) => unimplemented!(),
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
        },
        GraphqlId::UUID(x) => serde_json::Value::String(x),
    }
}