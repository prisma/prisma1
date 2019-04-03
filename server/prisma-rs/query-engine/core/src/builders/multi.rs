
use super::BuilderExt;
use crate::query_ast::MultiRelatedRecordQuery as QueryType;

pub struct Builder;

impl BuilderExt for Builder {
    type Output = QueryType;

    fn build(self) -> Self::Output {
        unimplemented!()
    }
}
