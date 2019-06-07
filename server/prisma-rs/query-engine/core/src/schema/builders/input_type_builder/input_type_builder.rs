use super::*;
use prisma_models::InternalDataModelRef;
use std::sync::{Arc, Weak};

/// Central builder for input types.
/// The InputTypeBuilder differs in one major aspect from the original implementation:
/// It doesn't use options to represent if a type should be rendered or not.
/// Instead, empty input types (i.e. without fields) will be rendered and must be filtered on higher layers.
#[derive(Debug)]
pub struct InputTypeBuilder<'a> {
    internal_data_model: InternalDataModelRef,
    input_type_cache: TypeRefCache<InputObjectType>,
    filter_object_builder: Weak<FilterObjectTypeBuilder<'a>>,
}

impl<'a> CachedBuilder<InputObjectType> for InputTypeBuilder<'a> {
    fn get_cache(&self) -> &TypeRefCache<InputObjectType> {
        &self.input_type_cache
    }

    fn into_strong_refs(self) -> Vec<Arc<InputObjectType>> {
        self.input_type_cache.into()
    }
}

impl<'a> InputTypeBuilderBase<'a> for InputTypeBuilder<'a> {
    fn get_filter_object_builder(&self) -> Arc<FilterObjectTypeBuilder<'a>> {
        self.filter_object_builder
            .upgrade()
            .expect("Invariant violation: Expected input type builder reference to be valid")
    }
}

impl<'a> InputBuilderExtensions for InputTypeBuilder<'a> {}
impl<'a> CreateInputTypeBuilderExtension<'a> for InputTypeBuilder<'a> {}
impl<'a> UpdateInputTypeBuilderExtension<'a> for InputTypeBuilder<'a> {}

impl<'a> InputTypeBuilder<'a> {
    pub fn new(
        internal_data_model: InternalDataModelRef,
        filter_object_builder: Weak<FilterObjectTypeBuilder<'a>>,
    ) -> Self {
        InputTypeBuilder {
            internal_data_model,
            input_type_cache: TypeRefCache::new(),
            filter_object_builder,
        }
    }
}
