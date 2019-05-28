use super::*;
use prisma_models::{
    FieldBehaviour, IdStrategy, InternalDataModelRef, ModelRef, RelationFieldRef, ScalarFieldRef, TypeIdentifier,
};
use std::{cell::RefCell, collections::HashMap, sync::Arc};

/// Caching utility.
macro_rules! cache {
    ($sel:ident, $name:expr, $obj:expr) => {
        $sel.input_type_cache.borrow_mut().insert($name, $obj);
    };
}

/// Loads and returns cached input object type if it is already cached.
macro_rules! return_cached {
    ($sel:ident, $name:expr) => {
        let existing_input_type = $sel.input_type_cache.borrow().get($name).cloned();
        if existing_input_type.is_some() {
            return existing_input_type.unwrap();
        }
    };
}

/// Central builder for input types.
/// The InputTypeBuilder differs in one major aspect from the original implementation: It doesn't use options
/// to represent if a type should be rendered or not.
/// Instead, empty (i.e. without fields) will be
pub struct InputTypeBuilder {
    internal_data_model: InternalDataModelRef,

    /// Cache acts as a circular dependency breaker.
    input_type_cache: RefCell<HashMap<String, InputObjectTypeRef>>,
}

impl InputTypeBuilder {
    pub fn new(internal_data_model: InternalDataModelRef) -> Self {
        InputTypeBuilder {
            internal_data_model,
            input_type_cache: RefCell::new(HashMap::new()),
        }
    }

    /// Builds the create input type (xCreateInput / xCreateWithout<y>Input)
    pub fn create_input_type(&self, model: ModelRef, parent_field: Option<RelationFieldRef>) -> InputObjectTypeRef {
        let name = match parent_field.as_ref().map(|pf| pf.related_field()) {
            Some(ref f) if !f.is_hidden => format!("{}CreateWithout{}Input", model.name, capitalize(f.name.as_ref())),
            _ => format!("{}CreateInput", model.name),
        };

        return_cached!(self, &name);

        let input_object = Arc::new(init_input_object_type(name.clone()));

        // Cache empty object for circuit breaking
        cache!(self, name, Arc::clone(&input_object));

        // Compute input fields for scalar fields.
        let scalar_fields: Vec<ScalarFieldRef> = model
            .fields()
            .scalar()
            .into_iter()
            .filter(|f| !f.is_created_at() && !f.is_updated_at() && !f.is_hidden && Self::do_filter(&f))
            .collect();

        let mut fields = self.scalar_input_fields(
            model.name.clone(),
            "Create",
            scalar_fields,
            |f: ScalarFieldRef| {
                match f.is_required && f.default_value.is_none() {
                    #[cfg_attr(rustfmt, rustfmt_skip)]
                    true if f.is_id() =>  match (f.behaviour.as_ref(), f.type_identifier) {
                        (Some(FieldBehaviour::Id { strategy: IdStrategy::Auto, .. }), TypeIdentifier::UUID)      => self.map_optional_input_type(f),
                        (Some(FieldBehaviour::Id { strategy: IdStrategy::Auto, .. }), TypeIdentifier::GraphQLID) => self.map_optional_input_type(f),
                        (None, TypeIdentifier::UUID)                                                             => self.map_optional_input_type(f),
                        (None, TypeIdentifier::GraphQLID)                                                        => self.map_optional_input_type(f),

                        (Some(FieldBehaviour::Id { strategy: IdStrategy::None, .. }), TypeIdentifier::GraphQLID) => self.map_required_input_type(f),
                        (Some(FieldBehaviour::Id { strategy: IdStrategy::None, .. }), TypeIdentifier::UUID)      => self.map_required_input_type(f),

                        _ => unreachable!(),
                    }

                    true => self.map_required_input_type(f),
                    false => self.map_optional_input_type(f),
                }
            },
        );

        // Compute input fields for relational fields.
        let mut relational_fields = self.relation_input_fields_create(Arc::clone(&model), parent_field.as_ref());
        fields.append(&mut relational_fields);

        input_object.set_fields(fields);
        input_object
    }

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

        // WIP: Risk of inifinite loop here
        let mut list_fields: Vec<InputField> = prefiltered_fields
            .into_iter()
            .filter(|f| f.is_list)
            .map(|f| {
                let name = f.name.clone();
                let set_name = format!("{}{}{}Input", model_name, input_object_name, f.name);
                let set_fields = vec![input_field("set", self.map_optional_input_type(f))];
                let set_input_type =
                    InputType::opt(InputType::object(Arc::new(input_object_type(set_name, set_fields))));

                input_field(name, set_input_type)
            })
            .collect();

        non_list_fields.append(&mut list_fields);
        non_list_fields
    }

    // For create input types only. Compute input fields for relational fields.
    // This recurses into create_input_type (via nested_create_input_field).
    fn relation_input_fields_create(
        &self,
        model: ModelRef,
        parent_field: Option<&RelationFieldRef>,
    ) -> Vec<InputField> {
        model
            .fields()
            .relation()
            .into_iter()
            .filter(|rf| !rf.is_hidden)
            .filter_map(|rf| {
                let related_model = rf.related_model();
                let related_field = rf.related_field();

                // Input object name
                let arity_part = if rf.is_list { "Many" } else { "One" };
                let without_part = if !related_field.is_hidden {
                    format!("Without{}", capitalize(rf.name.clone()))
                } else {
                    "".into()
                };

                let input_name = format!("{}Create{}{}Input", related_model.name, arity_part, without_part);
                let field_is_opposite_relation_field = parent_field
                    .as_ref()
                    .and_then(|pf| {
                        // TODO: The original version compared full case classes. Is this solution here enough?
                        if pf.related_field().name == rf.name {
                            Some(pf)
                        } else {
                            None
                        }
                    })
                    .is_some();

                if field_is_opposite_relation_field {
                    None
                } else {
                    let mut fields = vec![self.nested_create_input_field(Arc::clone(&rf))];
                    let nested_connect = self.nested_connect_input_field(Arc::clone(&rf));

                    append_opt(&mut fields, nested_connect);

                    let input_type = InputType::object(Arc::new(input_object_type(input_name, fields)));
                    let input_field = if rf.is_required {
                        input_field(rf.name.clone(), input_type)
                    } else {
                        input_field(rf.name.clone(), InputType::opt(input_type))
                    };

                    Some(input_field)
                }
            })
            .collect()
    }

    fn nested_create_input_field(&self, field: RelationFieldRef) -> InputField {
        let input_object = self.create_input_type(field.related_model(), Some(Arc::clone(&field)));
        let input_object = Self::wrap_list_input_object_type(input_object, field.is_list);

        input_field("create", input_object)
    }

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

    /// Wraps an input object type into a list object type.
    fn wrap_list_input_object_type(input: InputObjectTypeRef, as_list: bool) -> InputType {
        if as_list {
            InputType::opt(InputType::list(InputType::object(input)))
        } else {
            InputType::opt(InputType::object(input))
        }
    }

    /// Returns true if the field should be filtered for create input type building.
    fn do_filter(field: &ScalarFieldRef) -> bool {
        #[cfg_attr(rustfmt, rustfmt_skip)]
        match (field.behaviour.as_ref(), field.type_identifier) {
            _ if !field.is_id()                                                                      => true,

            (Some(FieldBehaviour::Id { strategy: IdStrategy::Auto, .. }), TypeIdentifier::Int)       => false,
            (None, TypeIdentifier::Int)                                                              => false,

            (Some(FieldBehaviour::Id { strategy: IdStrategy::Auto, .. }), TypeIdentifier::UUID)      => true,
            (Some(FieldBehaviour::Id { strategy: IdStrategy::Auto, .. }), TypeIdentifier::GraphQLID) => true,
            (None, TypeIdentifier::UUID)                                                             => true,
            (None, TypeIdentifier::GraphQLID)                                                        => true,

            (Some(FieldBehaviour::Id { strategy: IdStrategy::None, .. }), TypeIdentifier::Int)       => false,

            (Some(FieldBehaviour::Id { strategy: IdStrategy::None, .. }), TypeIdentifier::UUID)      => true,
            (Some(FieldBehaviour::Id { strategy: IdStrategy::None, .. }), TypeIdentifier::GraphQLID) => true,

            (Some(FieldBehaviour::Id { strategy: IdStrategy::Sequence, .. }), TypeIdentifier::Int)   => false,

            _ => panic!("Id Behaviour unhandled"),
        }
    }

    /// This function doesn't need to have caching (yet), because it doesn't trigger any code paths that recurse.
    /// Todo make sure this is truly the case.
    pub fn where_unique_object_type(&self, model: ModelRef) -> InputObjectTypeRef {
        let name = format!("{}WhereUniqueInput", model.name);

        let unique_fields: Vec<ScalarFieldRef> = model
            .fields()
            .scalar()
            .iter()
            .filter(|f| (f.is_unique() && !f.is_hidden) || f.is_id())
            .map(|f| Arc::clone(f))
            .collect();

        let input_object = init_input_object_type(name);

        let fields: Vec<InputField> = unique_fields
            .into_iter()
            .map(|f| input_field(f.name.clone(), self.map_optional_input_type(f)))
            .collect();

        input_object.set_fields(fields);
        Arc::new(input_object)
    }

    pub fn map_optional_input_type(&self, field: ScalarFieldRef) -> InputType {
        InputType::opt(self.map_required_input_type(field))
    }

    pub fn map_required_input_type(&self, field: ScalarFieldRef) -> InputType {
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

    pub fn map_enum_input_type(&self, field: &ScalarFieldRef) -> InputType {
        let internal_enum = field
            .internal_enum
            .as_ref()
            .expect("A field with TypeIdentifier Enum must always have an enum.");

        let et: EnumType = internal_enum.into();
        et.into()
    }
}
