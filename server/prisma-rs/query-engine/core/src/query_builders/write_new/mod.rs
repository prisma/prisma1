mod create;
mod delete;
mod delete_many;
mod nested;
mod update;
mod update_many;
mod upsert;
mod write_arguments;

pub use create::*;
pub use delete::*;
pub use delete_many::*;
pub use nested::*;
pub use update::*;
pub use update_many::*;
pub use upsert::*;
pub use write_arguments::*;

use super::*;
use connector::write_ast::WriteQuery;

pub enum WriteQueryBuilder {
    CreateBuilder(CreateBuilder),
    UpdateBuilder(UpdateBuilder),
    DeleteBuilder(DeleteBuilder),
    UpsertBuilder(UpsertBuilder),
    DeleteManyBuilder(DeleteManyBuilder),
    UpdateManyBuilder(UpdateManyBuilder),
}

impl Builder<WriteQuery> for WriteQueryBuilder {
    fn build(self) -> QueryBuilderResult<WriteQuery> {
        match self {
            WriteQueryBuilder::CreateBuilder(b) => b.build(),
            WriteQueryBuilder::UpdateBuilder(b) => b.build(),
            WriteQueryBuilder::DeleteBuilder(b) => b.build(),
            WriteQueryBuilder::UpsertBuilder(b) => b.build(),
            WriteQueryBuilder::DeleteManyBuilder(b) => b.build(),
            WriteQueryBuilder::UpdateManyBuilder(b) => b.build(),
        }
    }
}
