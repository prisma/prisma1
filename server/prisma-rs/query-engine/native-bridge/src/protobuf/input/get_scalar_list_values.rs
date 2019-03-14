use crate::protobuf::{prelude::*, InputValidation};
use prisma_common::PrismaResult;

impl InputValidation for GetScalarListValuesByNodeIds {
    fn validate(&self) -> PrismaResult<()> {
        Ok(())
    }
}
