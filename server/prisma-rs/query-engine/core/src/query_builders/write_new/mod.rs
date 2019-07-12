mod create;

pub use create::*;

use super::*;
use connector::write_ast::WriteQuery;

pub enum WriteQueryBuilder {
    CreateBuilder(CreateBuilder),
}

impl Builder<WriteQuery> for WriteQueryBuilder {
    fn build(self) -> QueryBuilderResult<WriteQuery> {
        match self {
            WriteQueryBuilder::CreateBuilder(b) => b.build(),
        }
    }
}
