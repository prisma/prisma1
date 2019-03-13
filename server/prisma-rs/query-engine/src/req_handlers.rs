mod graphql;
pub use graphql::{GraphQlBody, GraphQlRequestHandler};
use crate::context::PrismaContext;

use actix_web::HttpRequest;
use std::collections::HashMap;

pub trait RequestHandler {
    type Body;

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S, ctx: &PrismaContext);
}

pub struct PrismaRequest<T> {
    pub body: T,
    pub headers: HashMap<String, String>,
    pub path: String,
}

impl From<(GraphQlBody, HttpRequest)> for PrismaRequest<GraphQlBody> {
    fn from((gql, req): (GraphQlBody, HttpRequest)) -> Self {
        PrismaRequest {
            body: gql,
            path: req.path().into(),
            headers: req
                .headers()
                .iter()
                .map(|(k, v)| (format!("{}", k), v.to_str().unwrap().into()))
                .collect(),
        }
    }
}
