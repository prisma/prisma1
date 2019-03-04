use crate::{
    data_resolvers::{IntoSelectQuery, SelectQuery},
    protobuf::prelude::*,
};
use prisma_common::PrismaResult;

impl IntoSelectQuery for GetRelatedNodesInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        unimplemented!()
    }
}
