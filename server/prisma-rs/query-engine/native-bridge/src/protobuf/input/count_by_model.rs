use crate::protobuf::{prelude::*, InputValidation};
use prisma_common::PrismaResult;

impl InputValidation for CountByModelInput {
    fn validate(&self) -> PrismaResult<()> {
        Ok(())
    }
}
