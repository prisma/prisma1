use crate::data_resolvers::{DataResolver, SelectQuery, Sqlite};
use futures::{future, Async, Future, Poll};
use prisma_common::error::Error;
use prisma_models::prelude::*;
use tower_service::Service;

impl Service<SelectQuery> for Sqlite {
    type Response = (Vec<Vec<PrismaValue>>, Vec<String>);
    type Error = Error;
    type Future = Box<Future<Item = Self::Response, Error = Self::Error>>;

    fn poll_ready(&mut self) -> Poll<(), Self::Error> {
        Ok(Async::Ready(()))
    }

    fn call(&mut self, req: SelectQuery) -> Self::Future {
        let result = match self.select_nodes(req) {
            Ok(result) => future::ok(result),
            Err(error) => future::err(error),
        };

        Box::new(result)
    }
}
