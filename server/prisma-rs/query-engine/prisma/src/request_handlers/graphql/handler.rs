use super::protocol_adapter::GraphQLProtocolAdapter;
use crate::{context::PrismaContext, serializers::json, PrismaRequest, PrismaResult, RequestHandler};
use core::result_ir;
use graphql_parser as gql;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::{collections::HashMap, sync::Arc};

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
        let responses = match handle_graphql_query(req.into(), ctx) {
            Ok(responses) => responses,
            Err(err) => vec![err.into()],
        };

        json::serialize(responses)
    }
}

fn handle_graphql_query(
    req: PrismaRequest<GraphQlBody>,
    ctx: &PrismaContext,
) -> PrismaResult<Vec<result_ir::Response>> {
    debug!("Incoming GQL query: {:?}", &req.body.query);

    let gql_doc = gql::parse_query(&req.body.query)?;
    let query_doc = GraphQLProtocolAdapter::convert(gql_doc, req.body.operation_name)?;

    ctx.executor
        .execute(query_doc, Arc::clone(&ctx.query_schema))
        .map_err(|err| {
            debug!("{}", err);
            err.into()
        })
}
