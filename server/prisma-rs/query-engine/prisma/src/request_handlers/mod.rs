pub mod graphql;

pub use core::QuerySchemaRenderer;
pub use graphql::{GraphQlBody, GraphQlRequestHandler};

use crate::{context::PrismaContext, RequestContext};
use actix_web::HttpRequest;
use serde_json;
use std::collections::HashMap;
use std::sync::Arc;

pub trait RequestHandler {
    type Body;

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S, ctx: &PrismaContext) -> serde_json::Value;
}

pub struct PrismaRequest<T> {
    pub body: T,
    pub headers: HashMap<String, String>,
    pub path: String,
}

impl From<(GraphQlBody, HttpRequest<Arc<RequestContext>>)> for PrismaRequest<GraphQlBody> {
    fn from((gql, req): (GraphQlBody, HttpRequest<Arc<RequestContext>>)) -> Self {
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
