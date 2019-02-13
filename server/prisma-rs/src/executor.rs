use crate::{connectors::PrismaConnector, protobuf::prisma, PrismaResult};

pub trait QueryExecutor {
    fn query(self, connector: &PrismaConnector) -> PrismaResult<(Vec<prisma::Node>, Vec<String>)>;
}
