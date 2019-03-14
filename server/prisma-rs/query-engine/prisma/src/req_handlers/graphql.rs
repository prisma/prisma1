use super::{PrismaRequest, RequestHandler};
use crate::context::PrismaContext;
use crate::schema::Validatable;
use core::{PrismaQuery, QueryBuilder};
use graphql_parser as gql;
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

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S, ctx: &PrismaContext) {
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
    }
}
