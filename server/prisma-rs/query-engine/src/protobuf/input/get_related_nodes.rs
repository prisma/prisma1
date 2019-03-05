use crate::{
    data_resolvers::{IntoSelectQuery, SelectQuery},
    protobuf::{prelude::*, InputValidation},
};
use prisma_common::PrismaResult;

impl IntoSelectQuery for GetRelatedNodesInput {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        unimplemented!()
    }
}

impl InputValidation for GetRelatedNodesInput {
    fn validate(&self) -> PrismaResult<()> {
        Self::validate_args(&self.query_arguments)
    }
}
