pub mod read_ast;
pub mod write_ast;

use read_ast::*;
use write_ast::*;

#[derive(Debug, Clone)]
pub enum Query {
    Read(ReadQuery),
    Write(WriteQuery),
}
