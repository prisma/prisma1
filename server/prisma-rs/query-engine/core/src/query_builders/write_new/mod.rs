mod create;
mod nested;
mod update;
mod write_arguments;

pub use create::*;
pub use nested::*;
pub use update::*;
pub use write_arguments::*;

use super::*;
use connector::write_ast::WriteQuery;

pub enum WriteQueryBuilder {
    CreateBuilder(CreateBuilder),
    UpdateBuilder(UpdateBuilder),
}

impl Builder<WriteQuery> for WriteQueryBuilder {
    fn build(self) -> QueryBuilderResult<WriteQuery> {
        match self {
            WriteQueryBuilder::CreateBuilder(b) => b.build(),
            WriteQueryBuilder::UpdateBuilder(b) => b.build(),
        }
    }
}
