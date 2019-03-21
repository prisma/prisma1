use crate::protobuf::{prelude::*, BridgeResult, InputValidation};

impl InputValidation for CountByModelInput {
    fn validate(&self) -> BridgeResult<()> {
        Ok(())
    }
}
