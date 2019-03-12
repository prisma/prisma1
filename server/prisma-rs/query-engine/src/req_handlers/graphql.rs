use super::{PrismaRequest, RequestHandler};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Serialize, Deserialize)]
pub struct GraphQlBody {
    query: String,
    operation: String,
    variables: HashMap<String, String>,
}

pub struct GraphQlRequestHandler {}

impl RequestHandler for GraphQlRequestHandler {
    type Body = GraphQlBody;

    fn handle<S: Into<PrismaRequest<Self::Body>>>(&self, req: S) {
        unimplemented!()
    }
}
