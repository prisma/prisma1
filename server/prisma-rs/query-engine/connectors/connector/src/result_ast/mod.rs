mod read_result_ast;
mod write_result_ast;

pub use read_result_ast::*;
pub use write_result_ast::*;

pub enum QueryResult {
    Read(ReadQueryResult),
    Write(WriteQueryResult),
}