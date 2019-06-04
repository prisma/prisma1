mod create_input_type_extension;
mod input_type_builder;
mod update_input_type_extension;

pub use create_input_type_extension::*;
pub use input_type_builder::*;
pub use update_input_type_extension::*;

use super::*;
use prisma_models::{
    FieldBehaviour, IdStrategy, InternalDataModelRef, ModelRef, RelationFieldRef, ScalarFieldRef, TypeIdentifier,
};
use std::sync::Arc;

pub trait InputTypeBuilderBase: CachedBuilder<InputObjectType> + InputBuilderExtensions {
    /// Builds scalar input fields using the mapper and the given, prefiltered, scalar fields.
    /// The mapper is responsible for mapping the fields to input types.
    fn scalar_input_fields<T, F>(
        &self,
        model_name: String,
        input_object_name: T,
        prefiltered_fields: Vec<ScalarFieldRef>,
        field_mapper: F,
    ) -> Vec<InputField>
    where
        T: Into<String>,
        F: Fn(ScalarFieldRef) -> InputType,
    {
        let input_object_name = input_object_name.into();
        let mut non_list_fields: Vec<InputField> = prefiltered_fields
            .iter()
            .filter(|f| !f.is_list)
            .map(|f| input_field(f.name.clone(), field_mapper(Arc::clone(f))))
            .collect();

        let mut list_fields: Vec<InputField> = prefiltered_fields
            .into_iter()
            .filter(|f| f.is_list)
            .map(|f| {
                let name = f.name.clone();
                let set_name = dbg!(format!("{}{}{}Input", model_name, input_object_name, f.name));
                let input_object = match self.get_cache().get(&set_name) {
                    Some(t) => t,
                    None => {
                        let set_fields = vec![input_field("set", self.map_optional_input_type(f))];
                        let input_object = Arc::new(input_object_type(set_name.clone(), set_fields));
                        self.cache(set_name, Arc::clone(&input_object));
                        Arc::downgrade(&input_object)
                    }
                };

                let set_input_type = InputType::opt(InputType::object(input_object));
                input_field(name, set_input_type)
            })
            .collect();

        non_list_fields.append(&mut list_fields);
        non_list_fields
    }

    /// Builds the "connect" input field for a relation.
    fn nested_connect_input_field(&self, field: RelationFieldRef) -> Option<InputField> {
        if field.related_model().is_embedded {
            None
        } else {
            Some(self.where_input_field("connect", field))
        }
    }

    fn where_input_field<T>(&self, name: T, field: RelationFieldRef) -> InputField
    where
        T: Into<String>,
    {
        let input_type = self.where_unique_object_type(field.related_model());
        let input_type = Self::wrap_list_input_object_type(input_type, field.is_list);

        input_field(name.into(), input_type)
    }

    /// Wraps an input object type into an option list object type.
    fn wrap_list_input_object_type(input: InputObjectTypeRef, as_list: bool) -> InputType {
        if as_list {
            InputType::opt(InputType::list(InputType::object(input)))
        } else {
            InputType::opt(InputType::object(input))
        }
    }

    fn where_unique_object_type(&self, model: ModelRef) -> InputObjectTypeRef {
        let name = format!("{}WhereUniqueInput", model.name);
        return_cached!(self.get_cache(), &name);

        let input_object = Arc::new(init_input_object_type(name.clone()));
        self.cache(name, Arc::clone(&input_object));

        let unique_fields: Vec<ScalarFieldRef> = model
            .fields()
            .scalar()
            .iter()
            .filter(|f| (f.is_unique() && !f.is_hidden) || f.is_id())
            .map(|f| Arc::clone(f))
            .collect();

        let fields: Vec<InputField> = unique_fields
            .into_iter()
            .map(|f| input_field(f.name.clone(), self.map_optional_input_type(f)))
            .collect();

        input_object.set_fields(fields);
        Arc::downgrade(&input_object)
    }
}
