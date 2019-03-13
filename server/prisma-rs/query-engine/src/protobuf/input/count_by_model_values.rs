use crate::protobuf::{prelude::*, InputValidation};
use prisma_common::PrismaResult;

impl InputValidation for CountByModelValues {
    fn validate(&self) -> PrismaResult<()> {
        Ok(())
    }
}
