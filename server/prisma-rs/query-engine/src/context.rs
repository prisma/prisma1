use crate::req_handlers::GraphQlRequestHandler;
use crate::schema::{self, PrismaSchema};
use prisma_common::config::{self, PrismaConfig};

pub struct PrismaContext {
    pub config: PrismaConfig,
    pub schema: PrismaSchema,
}

impl PrismaContext {
    pub fn new() -> Self {
        Self {
            config: config::load().unwrap(),
            schema: schema::load_schema().unwrap(),
        }
    }
}
