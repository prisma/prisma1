use super::*;
use prisma_models::{InternalDataModelRef, ModelRef};

pub struct ArgumentBuilder {
    internal_data_model: InternalDataModelRef,
    input_type_builder: Weak<InputTypeBuilder>,
}

impl ArgumentBuilder {
    pub fn new(internal_data_model: InternalDataModelRef, input_type_builder: Weak<InputTypeBuilder>) -> Self {
        ArgumentBuilder {
            internal_data_model,
            input_type_builder,
        }
    }

    pub fn where_unique_argument(&self, model: ModelRef) -> Option<Argument> {
        let input_object_type = self.input_type_builder.into_arc().where_unique_object_type(model);

        if input_object_type.into_arc().is_empty() {
            None
        } else {
            Some(argument("where", InputType::object(input_object_type)))
        }
    }

    pub fn create_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        let input_object_type = self.input_type_builder.into_arc().create_input_type(model, None);

        if input_object_type.into_arc().is_empty() {
            None
        } else {
            Some(vec![argument("data", InputType::object(input_object_type))])
        }
    }

    pub fn update_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        // for {
        //   whereArg <- whereUniqueArgument(model)
        //   dataArg  <- inputTypesBuilder.inputObjectTypeForUpdate(model)
        // } yield {
        //   List(
        //     Argument[Any]("data", dataArg),
        //     whereArg
        //   )
        // }

        unimplemented!()
    }
}
