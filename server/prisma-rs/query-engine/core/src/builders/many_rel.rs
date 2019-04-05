use super::BuilderExt;
use crate::{query_ast::RelatedRecordQuery as QueryType, CoreResult};

pub struct Builder;

impl BuilderExt for Builder {
    type Output = QueryType;

    fn new() -> Self {
        unimplemented!()
    }

    fn build(self) -> CoreResult<Self::Output> {
        unimplemented!()
    }
}
