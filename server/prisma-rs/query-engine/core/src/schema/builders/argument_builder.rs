use super::*;
use prisma_models::ModelRef;

pub struct ArgumentBuilder<'a> {
    input_type_builder: Weak<InputTypeBuilder<'a>>,
    object_type_builder: Weak<ObjectTypeBuilder<'a>>,
}

/// Builder responsible for building the arguments required by the top-level fields of mutations.
impl<'a> ArgumentBuilder<'a> {
    pub fn new(
        input_type_builder: Weak<InputTypeBuilder<'a>>,
        object_type_builder: Weak<ObjectTypeBuilder<'a>>,
    ) -> Self {
        ArgumentBuilder {
            input_type_builder,
            object_type_builder,
        }
    }

    /// Builds "where" argument which input type is the where unique type of the input builder.
    pub fn where_unique_argument(&self, model: ModelRef) -> Option<Argument> {
        let input_object_type = self.input_type_builder.into_arc().where_unique_object_type(model);

        if input_object_type.into_arc().is_empty() {
            None
        } else {
            Some(argument("where", InputType::object(input_object_type)))
        }
    }

    /// Builds "data" argument intended for the create field.
    pub fn create_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        let input_object_type = self.input_type_builder.into_arc().create_input_type(model, None);

        if input_object_type.into_arc().is_empty() {
            None
        } else {
            Some(vec![argument("data", InputType::object(input_object_type))])
        }
    }

    /// Builds "where" (unique) argument intended for the delete field.
    pub fn delete_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        self.where_unique_argument(model).map(|arg| vec![arg])
    }

    /// Builds "where" (unique) and "data" arguments intended for the update field.
    pub fn update_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        self.where_unique_argument(Arc::clone(&model)).map(|unique_arg| {
            let input_object = self.input_type_builder.into_arc().update_input_type(model);
            let input_object_type = InputType::object(input_object);

            vec![argument("data", input_object_type), unique_arg]
        })
    }

    /// Builds "where" (unique), "create", and "update" arguments intended for the upsert field.
    pub fn upsert_arguments(&self, model: ModelRef) -> Option<Vec<Argument>> {
        self.where_unique_argument(Arc::clone(&model))
            .and_then(|where_unique_arg| {
                let update_type = self.input_type_builder.into_arc().update_input_type(Arc::clone(&model));
                let create_type = self
                    .input_type_builder
                    .into_arc()
                    .create_input_type(Arc::clone(&model), None);

                if update_type.into_arc().is_empty() || create_type.into_arc().is_empty() {
                    None
                } else {
                    Some(vec![
                        where_unique_arg,
                        argument("create", InputType::object(create_type)),
                        argument("update", InputType::object(update_type)),
                    ])
                }
            })
    }

    /// Builds "where" and "data" arguments intended for the update many field.
    pub fn update_many_arguments(&self, model: ModelRef) -> Vec<Argument> {
        let update_object = self
            .input_type_builder
            .into_arc()
            .update_many_input_type(Arc::clone(&model));

        let where_arg = self.object_type_builder.into_arc().where_argument(&model);

        vec![argument("data", InputType::object(update_object)), where_arg]
    }

    /// Builds "where" argument intended for the delete many field.
    pub fn delete_many_arguments(&self, model: ModelRef) -> Vec<Argument> {
        let where_arg = self.object_type_builder.into_arc().where_argument(&model);

        vec![where_arg]
    }
}
