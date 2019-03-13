use crate::req_handlers::GraphQlRequestHandler;
use crate::schema::{self, GraphQlSchema};
use prisma_common::config::{self, PrismaConfig};
use prisma_models::SchemaRef;

pub struct PrismaContext {
    pub config: PrismaConfig,
    pub schema: SchemaRef,
}

impl PrismaContext {
    pub fn new() -> Self {
        Self {
            config: config::load().unwrap(),
            schema: schema::load_schema().unwrap(),
        }
    }
}
