use crate::{connector::PrismaConnector, protobuf::prisma, PrismaResult};

pub trait QueryExecutor {
    fn query(self, connector: &PrismaConnector) -> PrismaResult<(Vec<prisma::Node>, Vec<String>)>;
}
