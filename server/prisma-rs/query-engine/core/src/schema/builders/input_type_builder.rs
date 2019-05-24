use super::*;
use prisma_models::{InternalDataModelRef, ModelRef, ScalarField, TypeIdentifier};
use std::sync::Arc;

pub struct InputTypeBuilder {
    internal_data_model: InternalDataModelRef,
}

impl InputTypeBuilder {
    pub fn new(internal_data_model: InternalDataModelRef) -> Self {
        InputTypeBuilder { internal_data_model }
    }

    // Wip: I assume that we need to cache intermediate objects again here to prevent infinite loops.
    pub fn where_unique_object_type(&self, model: ModelRef) -> Option<InputObjectTypeRef> {
        let unique_fields: Vec<Arc<ScalarField>> = model
            .fields()
            .scalar()
            .iter()
            .filter(|f| (f.is_unique && !f.is_hidden) || f.is_id())
            .map(|f| Arc::clone(f))
            .collect();

        if unique_fields.len() > 0 {
            let input_object = init_input_object_type(format!("{}WhereUniqueInput", model.name));
            let fields: Vec<InputField> = unique_fields
                .into_iter()
                .map(|f| input_field(f.name.clone(), self.map_optional_input_type(f)))
                .collect();

            input_object.set_fields(fields);
            Some(Arc::new(input_object)) // wip: potential caching here
        } else {
            None
        }
    }

    pub fn map_optional_input_type(&self, field: Arc<ScalarField>) -> InputType {
        InputType::opt(self.map_required_input_type(field))
    }

    pub fn map_required_input_type(&self, field: Arc<ScalarField>) -> InputType {
        let typ = match field.type_identifier {
            TypeIdentifier::String => InputType::string(),
            TypeIdentifier::Int => InputType::int(),
            TypeIdentifier::Float => InputType::float(),
            TypeIdentifier::Boolean => InputType::boolean(),
            TypeIdentifier::GraphQLID => InputType::id(),
            TypeIdentifier::UUID => InputType::uuid(),
            TypeIdentifier::DateTime => InputType::date_time(),
            TypeIdentifier::Json => InputType::json(),
            TypeIdentifier::Enum => self.map_enum_input_type(&field).into(),
            TypeIdentifier::Relation => unreachable!(), // A scalar field can't be a relation.
        };

        if field.is_list {
            InputType::list(typ)
        } else {
            typ
        }
    }

    pub fn map_enum_input_type(&self, field: &Arc<ScalarField>) -> InputType {
        let internal_enum = field
            .internal_enum
            .as_ref()
            .expect("A field with TypeIdentifier Enum must always have an enum.");

        let et: EnumType = internal_enum.into();
        et.into()
    }
}
