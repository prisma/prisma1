use super::*;
use prisma_models::{ModelRef, RelationFieldRef, ScalarFieldRef};

pub trait UpdateInputTypeBuilderExtension: InputTypeBuilderBase + CreateInputTypeBuilderExtension {
    fn update_input_type(&self, model: ModelRef) -> InputObjectTypeRef {
        let name = format!("{}UpdateInput", model.name.clone());
        return_cached!(self.get_cache(), &name);

        let input_object = Arc::new(init_input_object_type(name.clone()));
        self.cache(name, Arc::clone(&input_object));

        // Compute input fields for scalar fields.
        let mut fields = self.scalar_input_fields_for_update(Arc::clone(&model));

        // Compute input fields for relational fields.
        let mut relational_fields = self.relation_input_fields_for_update(Arc::clone(&model), None);
        fields.append(&mut relational_fields);

        input_object.set_fields(fields);
        Arc::downgrade(&input_object)
    }

    fn scalar_input_fields_for_update(&self, model: ModelRef) -> Vec<InputField> {
        let scalar_fields: Vec<ScalarFieldRef> = model
            .fields()
            .scalar()
            .into_iter()
            .filter(|f| f.is_writable())
            .collect();

        self.scalar_input_fields(model.name.clone(), "Update", scalar_fields, |f: ScalarFieldRef| {
            self.map_optional_input_type(f)
        })
    }

    /// For update input types only. Compute input fields for relational fields.
    /// This recurses into create_input_type (via nested_create_input_field).
    /// Todo: This code is fairly similar to "create" relation computation. Let's see if we can dry it up.
    fn relation_input_fields_for_update(
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

                // Compute input object name
                let arity_part = match (rf.is_list, rf.is_required) {
                    (true, _) => "Many",
                    (false, true) => "OneRequired",
                    (false, false) => "One",
                };

                let without_part = if !related_field.is_hidden {
                    format!("Without{}", capitalize(rf.name.clone()))
                } else {
                    "".into()
                };

                let input_name = format!("{}Update{}{}Input", related_model.name, arity_part, without_part);
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
                    let input_object = match self.get_cache().get(&input_name) {
                        Some(t) => t,
                        None => {
                            let input_object = Arc::new(init_input_object_type(input_name.clone()));
                            self.cache(input_name, Arc::clone(&input_object));

                            let mut fields = vec![self.nested_create_input_field(Arc::clone(&rf))];

                            append_opt(&mut fields, self.nested_connect_input_field(Arc::clone(&rf)));
                            append_opt(&mut fields, self.nested_set_input_field(Arc::clone(&rf)));
                            append_opt(&mut fields, self.nested_disconnect_input_field(Arc::clone(&rf)));

                            // wip delete input field

                            fields.push(self.nested_update_input_field(Arc::clone(&rf)));

                            // wip nested update many input field
                            // wip nested delete many input field
                            // wip nested upsert input field

                            input_object.set_fields(fields);
                            Arc::downgrade(&input_object)
                        }
                    };

                    let input_type = InputType::object(input_object);
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

    /// Builds "set" field for nested updates (on relation fields).
    fn nested_set_input_field(&self, field: RelationFieldRef) -> Option<InputField> {
        match (field.related_model().is_embedded, field.is_list) {
            (true, _) => None,
            (false, true) => Some(self.where_input_field("set", field)),
            (false, false) => None,
        }
    }

    /// Builds "disconnect" field for nested updates (on relation fields).
    fn nested_disconnect_input_field(&self, field: RelationFieldRef) -> Option<InputField> {
        match (field.related_model().is_embedded, field.is_list, field.is_required) {
            (true, _, _) => None,
            (false, true, _) => Some(self.where_input_field("disconnect", field)),
            (false, false, false) => Some(input_field("disconnect", InputType::opt(InputType::boolean()))),
            (false, false, true) => None,
        }
    }

    /// Builds "delete" field for nested updates (on relation fields).
    fn nested_delete_input_field(&self, field: RelationFieldRef) -> Option<InputField> {
        match (field.is_list, field.is_required) {
            (true, _) => Some(self.where_input_field("delete", field)),
            (false, false) => Some(input_field("delete", InputType::opt(InputType::boolean()))),
            (false, true) => None,
        }
    }

    fn nested_update_input_field(&self, field: RelationFieldRef) -> InputField {
        let input_object = self.input_object_type_nested_update(Arc::clone(&field));
        let input_object = Self::wrap_list_input_object_type(input_object, field.is_list);

        input_field("update", input_object)
    }

    fn input_object_type_nested_update(&self, parent_field: RelationFieldRef) -> InputObjectTypeRef {
        let related_model = parent_field.related_model();
        let nested_input_object = self.nested_update_data(Arc::clone(&parent_field));

        if parent_field.is_list {
            let where_input_object = self.where_unique_object_type(Arc::clone(&related_model));
            let type_name = if parent_field.related_field().is_hidden {
                format!("{}UpdateWithWhereUniqueNestedInput", related_model.name.clone())
            } else {
                format!(
                    "{}UpdateWithWhereUniqueWithout{}Input",
                    related_model.name.clone(),
                    capitalize(parent_field.related_field().name.clone())
                )
            };

            return_cached!(self.get_cache(), &type_name);
            let input_object = Arc::new(init_input_object_type(type_name.clone()));
            self.cache(type_name, Arc::clone(&input_object));

            let fields = vec![
                input_field("where", InputType::object(where_input_object)),
                input_field("data", InputType::object(nested_input_object)),
            ];

            input_object.set_fields(fields);
            Arc::downgrade(&input_object)
        } else {
            nested_input_object
        }
    }

    fn nested_update_data(&self, parent_field: RelationFieldRef) -> InputObjectTypeRef {
        let related_model = parent_field.related_model();
        let type_name = if parent_field.related_field().is_hidden {
            format!("{}UpdateDataInput", related_model.name.clone())
        } else {
            format!(
                "{}UpdateWithout{}DataInput",
                related_model.name.clone(),
                capitalize(parent_field.related_field().name.clone())
            )
        };

        return_cached!(self.get_cache(), &type_name);

        let input_object = Arc::new(init_input_object_type(type_name.clone()));
        self.cache(type_name, Arc::clone(&input_object));

        let mut fields = self.scalar_input_fields_for_update(Arc::clone(&related_model));
        let mut relational_input_fields =
            self.relation_input_fields_for_update(Arc::clone(&related_model), Some(&parent_field));

        fields.append(&mut relational_input_fields);
        input_object.set_fields(fields);

        Arc::downgrade(&input_object)
    }
}
