use crate::{
    //cursor_condition::CursorCondition,
    database_executor::{IntoSelectQuery, SelectQuery},
    //ordering::Ordering,
    //protobuf::filter::IntoFilter,
    protobuf::{prelude::*, InputValidation},
};

use prisma_common::PrismaResult;
//use prisma_models::prelude::*;
//use prisma_query::ast::*;

impl IntoSelectQuery for GetScalarListValues {
    fn into_select_query(self) -> PrismaResult<SelectQuery> {
        unimplemented!()
    }
}

impl InputValidation for GetScalarListValues {
    fn validate(&self) -> PrismaResult<()> {
        unimplemented!()
    }
}
