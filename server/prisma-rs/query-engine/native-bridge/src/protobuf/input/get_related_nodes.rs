use crate::protobuf::{prelude::*, BridgeResult, InputValidation};

impl InputValidation for GetRelatedNodesInput {
    fn validate(&self) -> BridgeResult<()> {
        Self::validate_args(&self.query_arguments)
    }
}
