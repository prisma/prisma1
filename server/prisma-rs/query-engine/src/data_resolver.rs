mod sql;

use crate::node_selector::NodeSelector;
use prisma_common::PrismaResult;
use prisma_models::{SelectedFields, SingleNode};
pub use sql::*;

pub trait DataResolver {
    fn get_node_by_where(
        &self,
        node_selector: NodeSelector,
        selected_fields: SelectedFields,
    ) -> PrismaResult<Option<SingleNode>>;
}
