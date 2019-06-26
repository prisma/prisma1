use super::protocol_adapter::GraphQLProtocolAdapter;
use crate::{
    context::PrismaContext, error::PrismaError, serializers::json, PrismaRequest, PrismaResult, RequestHandler,
};
use core::result_ir::{self as rir, Builder};
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
        handle_safely(req.into(), ctx)
    }
}

fn handle_safely(req: PrismaRequest<GraphQlBody>, ctx: &PrismaContext) -> Value {
    debug!("Incoming GQL query: {:?}", &req.body.query);

    let query_doc = match gql::parse_query(&req.body.query) {
        Ok(doc) => doc,
        Err(err) => {
            let ir = vec![rir::Response::Error(format!("{:?}", err))];
            return json::serialize(ir);
        }
    };

    // --- New flow ---
    let query_schema = Arc::clone(&ctx.query_schema);
    let query_doc = GraphQLProtocolAdapter::convert(query_doc, req.body.operation_name).unwrap();
    //    let result_set = QueryPipeline::new(query_schema, query_doc).execute().unwrap();

    //    Ok(json::serialize(result_set))
    unimplemented!()

    // --- Old flow ---
    //    let rb = RootBuilder {
    //        query: query_doc,
    //        query_schema: Arc::clone(&ctx.query_schema),
    //        operation_name: req.body.operation_name,
    //    };
    //
    //    let queries = rb.build();
    //
    //    let ir = match queries {
    //        Ok(q) => match ctx.executor.exec_all(q) {
    //            Ok(results) => results
    //                .into_iter()
    //                .fold(Builder::new(), |builder, result| builder.add(result))
    //                .build(),
    //            Err(err) => vec![rir::Response::Error(format!("{:?}", err))], // This is merely a workaround
    //        },
    //        Err(err) => vec![rir::Response::Error(format!("{:?}", err))], // This is merely a workaround
    //    };
    //
    //    Ok(json::serialize(ir))
}
