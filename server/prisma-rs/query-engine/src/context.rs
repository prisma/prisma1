use crate::req_handlers::{GraphQlRequestHandler, RequestHandler};
use crate::schema::{self, PrismaSchema};
use prisma_common::config::{self, PrismaConfig};

pub struct PrismaContext {
    pub config: PrismaConfig,
    pub request_handler: GraphQlRequestHandler,
    pub schema: PrismaSchema,
}

impl PrismaContext {
    pub fn new() -> Self {
        Self {
            config: config::load().unwrap(),
            request_handler: GraphQlRequestHandler {},
            schema: schema::load_schema().unwrap(),
        }
    }
}
