mod graphql;
pub use graphql::GraphQlBody;

use actix_web::HttpRequest;
use std::collections::HashMap;


trait RequestHandler<T> {
    fn handle<S: Into<PrismaRequest<T>>>(req: S);
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
