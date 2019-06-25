mod read_ast;
mod write_ast;

pub use read_ast::*;
pub use write_ast::*;

#[derive(Debug, Clone)]
pub enum Query {
    Read(ReadQuery),
    Write(WriteQuery),
}
