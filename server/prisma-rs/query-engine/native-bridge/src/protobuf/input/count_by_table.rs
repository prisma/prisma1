use crate::protobuf::{prelude::*, BridgeResult, InputValidation};

impl InputValidation for CountByTableInput {
    fn validate(&self) -> BridgeResult<()> {
        Ok(())
    }
}
