use crate::req_handlers::{GraphQlRequestHandler, RequestHandler};
use prisma_common::config::{self, PrismaConfig};

pub struct ServerDependencies {
    pub config: PrismaConfig,
    pub request_handler: GraphQlRequestHandler,
}

impl ServerDependencies {
    pub fn new() -> Self {
        ServerDependencies {
            config: config::load().unwrap(),
            request_handler: GraphQlRequestHandler {},
        }
    }
}
