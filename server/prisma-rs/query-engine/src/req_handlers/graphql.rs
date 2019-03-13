use super::{PrismaRequest, RequestHandler};
use crate::context::PrismaContext;
use crate::schema::Validatable;
use graphql_parser as gql;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct GraphQlBody {
    query: String,
    operation: String,
    variables: HashMap<String, String>,
}

pub struct GraphQlRequestHandler;

impl RequestHandler for GraphQlRequestHandler {
    type Body = GraphQlBody;

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S, ctx: &PrismaContext) {
        let ast = gql::parse_query(&req.into().body.query).unwrap();

        if let Ok(()) = ctx.schema.validate(&ast) {
            dbg!(ast);
        }
    }
}
