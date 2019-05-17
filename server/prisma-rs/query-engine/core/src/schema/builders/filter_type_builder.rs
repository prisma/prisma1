use super::*;
use prisma_models::ModelRef;

pub struct FilterObjectTypeBuilder {
  pub model: ModelRef,
}

impl FilterObjectTypeBuilder {
  pub fn build(&self, capabilities: &SupportedCapabilities) -> InputType {
    if capabilities.has(ConnectorCapability::MongoJoinRelationLinks) {
      self.mongo_filter_object()
    } else {
      self.filter_object()
    }
  }

  fn filter_object(&self) -> InputType {
    unimplemented!()
  }

  fn mongo_filter_object(&self) -> InputType {
    unimplemented!()
  }
}
