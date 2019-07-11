mod create;

pub use create::*;

use super::*;
use connector::write_ast::WriteQuery;

pub trait Builder {
    fn build(self) -> QueryBuilderResult<WriteQuery>;
}

pub enum WriteQueryBuilder {
    CreateBuilder(CreateBuilder),
}