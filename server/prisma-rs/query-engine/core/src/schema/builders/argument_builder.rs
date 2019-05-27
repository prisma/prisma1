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
        self.input_type_builder
            .where_unique_object_type(model)
            .map(|input| argument("where", InputType::object(input)))
    }

    pub fn create_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        // inputTypesBuilder.inputObjectTypeForCreate(model).map { args =>
        //   List(Argument[Any]("data", args))
        // }
        // self.input_type_builder.create_input_type()
        unimplemented!()
    }
}
