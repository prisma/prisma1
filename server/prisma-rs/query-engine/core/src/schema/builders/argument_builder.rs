use super::*;
use prisma_models::{InternalDataModelRef, ModelRef};

pub struct ArgumentBuilder<'a> {
    internal_data_model: InternalDataModelRef,
    input_type_builder: Weak<InputTypeBuilder<'a>>,
    object_type_builder: Weak<ObjectTypeBuilder<'a>>,
}

impl<'a> ArgumentBuilder<'a> {
    pub fn new(
        internal_data_model: InternalDataModelRef,
        input_type_builder: Weak<InputTypeBuilder<'a>>,
        object_type_builder: Weak<ObjectTypeBuilder<'a>>,
    ) -> Self {
        ArgumentBuilder {
            internal_data_model,
            input_type_builder,
            object_type_builder,
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

    pub fn delete_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        self.where_unique_argument(model).map(|arg| vec![arg])
    }

    pub fn update_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        self.where_unique_argument(Arc::clone(&model)).map(|unique_arg| {
            let input_object = self.input_type_builder.into_arc().update_input_type(model);
            let input_object_type = InputType::object(input_object);

            vec![argument("data", input_object_type), unique_arg]
        })
    }

    pub fn upsert_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        self.where_unique_argument(Arc::clone(&model)).map(|where_unique_arg| {
            let update_type = self.input_type_builder.into_arc().update_input_type(Arc::clone(&model));
            let create_type = self
                .input_type_builder
                .into_arc()
                .create_input_type(Arc::clone(&model), None);

            vec![
                where_unique_arg,
                argument("create", InputType::object(create_type)),
                argument("update", InputType::object(update_type)),
            ]
        })
    }

    pub fn update_many_arguments(&self, model: ModelRef) -> Vec<Argument> {
        let update_object = self
            .input_type_builder
            .into_arc()
            .update_many_input_type(Arc::clone(&model));

        let where_arg = self.object_type_builder.into_arc().where_argument(&model);

        vec![argument("data", InputType::object(update_object)), where_arg]
    }

    pub fn delete_many_arguments(&self, model: ModelRef) -> Vec<Argument> {
        let where_arg = self.object_type_builder.into_arc().where_argument(&model);

        vec![where_arg]
    }
}
