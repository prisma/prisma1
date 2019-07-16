pub mod read_ast;
pub mod write_ast;

pub use read_ast::*;
pub use write_ast::*;

#[derive(Debug, Clone)]
pub enum Query {
    Read(ReadQuery),
    Write(WriteQuery, ReadQuery), // Pair of dependent queries. Temporary until query execution redesign.
}
