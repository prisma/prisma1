use crate::protobuf::{prelude::*, BridgeResult, InputValidation};

impl InputValidation for GetNodesInput {
    fn validate(&self) -> BridgeResult<()> {
        Self::validate_args(&self.query_arguments)
    }
}
