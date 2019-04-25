use super::{PrismaRequest, RequestHandler};
use crate::{context::PrismaContext, data_model::Validatable, error::PrismaError, PrismaResult};
use core::{ir::Builder, ReadQuery, RootBuilder};
use graphql_parser as gql;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

use serde_json::{Map, Value};

use crate::serializer::json;

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
                map.insert("reason".into(), format!("{}", err).into());
                json_envelope("error", map)
            }
        }
    }
}

fn handle_safely(req: PrismaRequest<GraphQlBody>, ctx: &PrismaContext) -> PrismaResult<Value> {
    debug!("Incoming GQL query: {:?}", &req.body.query);
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

    let rb = RootBuilder {
        query: query_doc,
        schema: ctx.schema.clone(),
        operation_name: req.body.operation_name,
    };

    let queries: Vec<ReadQuery> = rb.build()?;
    let ir = dbg!(ctx.read_query_executor.execute(&queries)?)
        .into_iter()
        .fold(Builder::new(), |builder, result| builder.add(result))
        .build();

    Ok(json::serialize(ir))
}

/// Create a json envelope
fn json_envelope(id: &str, map: serde_json::Map<String, Value>) -> Value {
    let mut envelope = JsonMap::new();
    envelope.insert(id.to_owned(), Value::Object(map));
    Value::Object(envelope)
}
