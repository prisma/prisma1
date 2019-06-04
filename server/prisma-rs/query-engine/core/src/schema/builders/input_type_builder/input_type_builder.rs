use super::*;
use prisma_models::{
    FieldBehaviour, IdStrategy, InternalDataModelRef, ModelRef, RelationFieldRef, ScalarFieldRef, TypeIdentifier,
};
use std::sync::Arc;

/// Central builder for input types.
/// The InputTypeBuilder differs in one major aspect from the original implementation:
/// It doesn't use options to represent if a type should be rendered or not.
/// Instead, empty input types (i.e. without fields) will be rendered and must be filtered on higher layers.
#[derive(Debug)]
pub struct InputTypeBuilder {
    internal_data_model: InternalDataModelRef,
    input_type_cache: TypeRefCache<InputObjectType>,
}

impl CachedBuilder<InputObjectType> for InputTypeBuilder {
    fn get_cache(&self) -> &TypeRefCache<InputObjectType> {
        &self.input_type_cache
    }

    fn into_strong_refs(self) -> Vec<Arc<InputObjectType>> {
        self.input_type_cache.into()
    }
}

impl InputBuilderExtensions for InputTypeBuilder {}
impl UpdateInputTypeBuilderExtension for InputTypeBuilder {}
impl CreateInputTypeBuilderExtension for InputTypeBuilder {}

impl InputTypeBuilderBase for InputTypeBuilder {}

impl InputTypeBuilder {
    pub fn new(internal_data_model: InternalDataModelRef) -> Self {
        InputTypeBuilder {
            internal_data_model,
            input_type_cache: TypeRefCache::new(),
        }
    }
}
