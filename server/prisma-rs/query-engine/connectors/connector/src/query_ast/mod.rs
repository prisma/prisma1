pub mod read_ast;
pub mod write_ast;

pub use read_ast::*;
pub use write_ast::*;

use prisma_models::ModelRef;

#[derive(Debug, Clone)]
pub enum Query {
    Read(ReadQuery),
    Write(WriteQuery),
}

/// This is purely a workaround for the query execution
/// requiring models for dependent queries.
pub trait ModelExtractor {
    fn extract_model(&self) -> Option<ModelRef>;
}

impl ModelExtractor for Query {
    fn extract_model(&self) -> Option<ModelRef> {
        match self {
            Query::Read(rq) => rq.extract_model(),
            Query::Write(wq) => wq.extract_model(),
        }
    }
}
