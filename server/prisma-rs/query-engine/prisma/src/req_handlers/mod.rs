mod graphql;
use crate::context::PrismaContext;
pub use graphql::{GraphQlBody, GraphQlRequestHandler};

use super::HttpHandler;
use actix_web::HttpRequest;
use std::collections::HashMap;
use std::sync::Arc;

pub trait RequestHandler {
    type Body;

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S, ctx: &PrismaContext);
}

pub struct PrismaRequest<T> {
    pub body: T,
    pub headers: HashMap<String, String>,
    pub path: String,
}

impl From<(GraphQlBody, HttpRequest<Arc<HttpHandler>>)> for PrismaRequest<GraphQlBody> {
    fn from((gql, req): (GraphQlBody, HttpRequest<Arc<HttpHandler>>)) -> Self {
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
