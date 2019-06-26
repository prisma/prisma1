use crate::{query_ir::QueryDocument, CoreResult, QuerySchemaRef};
use connector::Query;

pub struct QueryBuilder {
    pub query_doc: QueryDocument,
    pub query_schema: QuerySchemaRef,
}

impl QueryBuilder {
    pub fn build() -> CoreResult<Vec<Query>> {
        unimplemented!()
    }
}
