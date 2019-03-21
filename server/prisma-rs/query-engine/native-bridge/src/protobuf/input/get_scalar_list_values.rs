use crate::protobuf::{prelude::*, BridgeResult, InputValidation};

impl InputValidation for GetScalarListValuesByNodeIds {
    fn validate(&self) -> BridgeResult<()> {
        Ok(())
    }
}
