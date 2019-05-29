use super::*;
use prisma_models::{InternalDataModelRef, ModelRef};
use std::sync::Arc;

pub struct ArgumentBuilder {
    internal_data_model: InternalDataModelRef,
    input_type_builder: Arc<InputTypeBuilder>,
}

impl ArgumentBuilder {
    pub fn new(internal_data_model: InternalDataModelRef, input_type_builder: Arc<InputTypeBuilder>) -> Self {
        ArgumentBuilder {
            internal_data_model,
            input_type_builder,
        }
    }

    pub fn where_unique_argument(&self, model: ModelRef) -> Option<Argument> {
        let input_object_type = self.input_type_builder.where_unique_object_type(model);

        if input_object_type.into_arc().is_empty() {
            None
        } else {
            Some(argument("where", InputType::object(input_object_type)))
        }
    }

    pub fn create_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        let input_object_type = self.input_type_builder.create_input_type(model, None);

        if input_object_type.into_arc().is_empty() {
            None
        } else {
            Some(vec![argument("data", InputType::object(input_object_type))])
        }
    }
}
