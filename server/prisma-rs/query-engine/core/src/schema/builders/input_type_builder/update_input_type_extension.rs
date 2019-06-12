use super::*;
use prisma_models::{ModelRef, RelationFieldRef, ScalarFieldRef};

pub trait UpdateInputTypeBuilderExtension<'a>: InputTypeBuilderBase<'a> + CreateInputTypeBuilderExtension<'a> {
    /// Builds "<x>UpdateInput" input object type.
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

    /// Builds "<x>UpdateManyMutationInput" input object type.
    fn update_many_input_type(&self, model: ModelRef) -> InputObjectTypeRef {
        let object_name = format!("{}UpdateManyMutationInput", model.name);
        return_cached!(self.get_cache(), &object_name);

        let input_fields = self.scalar_input_fields_for_update(Arc::clone(&model));
        let input_object = Arc::new(input_object_type(object_name.clone(), input_fields));

        self.cache(object_name, Arc::clone(&input_object));
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

                let without_part = if related_field.is_hidden {
                    "".into()
                } else {
                    format!("Without{}", capitalize(related_field.name.clone()))
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
                            append_opt(&mut fields, self.nested_delete_input_field(Arc::clone(&rf)));
                            fields.push(self.nested_update_input_field(Arc::clone(&rf)));
                            append_opt(&mut fields, self.nested_update_many_field(Arc::clone(&rf)));
                            append_opt(&mut fields, self.nested_delete_many_field(Arc::clone(&rf)));
                            append_opt(&mut fields, self.nested_upsert_field(Arc::clone(&rf)));

                            input_object.set_fields(fields);
                            Arc::downgrade(&input_object)
                        }
                    };

                    Some(input_field(
                        rf.name.clone(),
                        InputType::opt(InputType::object(input_object)),
                    ))
                }
            })
            .collect()
    }

    /// Builds "upsert" field for nested updates (on relation fields).
    fn nested_upsert_field(&self, field: RelationFieldRef) -> Option<InputField> {
        self.nested_upsert_input_object(Arc::clone(&field)).map(|input_object| {
            let input_type = Self::wrap_list_input_object_type(input_object, field.is_list);
            input_field("upsert", input_type)
        })
    }

    fn nested_upsert_input_object(&self, parent_field: RelationFieldRef) -> Option<InputObjectTypeRef> {
        let nested_update_data_object = self.nested_update_data(Arc::clone(&parent_field));

        if parent_field.is_list {
            self.nested_upsert_list_input_object(parent_field, nested_update_data_object)
        } else {
            self.nested_upsert_nonlist_input_object(parent_field, nested_update_data_object)
        }
    }

    /// Builds "<x>UpsertWithWhereUniqueNestedInput" / "<x>UpsertWithWhereUniqueWithout<y>Input" input object types.
    fn nested_upsert_list_input_object(
        &self,
        parent_field: RelationFieldRef,
        update_object: InputObjectTypeRef,
    ) -> Option<InputObjectTypeRef> {
        let related_model = parent_field.related_model();
        let where_object = self.where_unique_object_type(Arc::clone(&related_model));
        let create_object = self.create_input_type(Arc::clone(&related_model), Some(Arc::clone(&parent_field)));

        if where_object.into_arc().is_empty() || create_object.into_arc().is_empty() {
            return None;
        }

        let type_name = if parent_field.related_field().is_hidden {
            format!("{}UpsertWithWhereUniqueNestedInput", related_model.name.clone())
        } else {
            format!(
                "{}UpsertWithWhereUniqueWithout{}Input",
                related_model.name.clone(),
                capitalize(parent_field.related_field().name.clone())
            )
        };

        match self.get_cache().get(&type_name) {
            None => {
                let input_object = Arc::new(init_input_object_type(type_name.clone()));
                self.cache(type_name, Arc::clone(&input_object));

                let fields = vec![
                    input_field("where", InputType::object(where_object)),
                    input_field("update", InputType::object(update_object)),
                    input_field("create", InputType::object(create_object)),
                ];

                input_object.set_fields(fields);
                Some(Arc::downgrade(&input_object))
            }
            x => x,
        }
    }

    /// Builds "<x>UpsertNestedInput" / "<x>UpsertWithout<y>Input" input object types.
    fn nested_upsert_nonlist_input_object(
        &self,
        parent_field: RelationFieldRef,
        update_object: InputObjectTypeRef,
    ) -> Option<InputObjectTypeRef> {
        let related_model = parent_field.related_model();
        let create_object = self.create_input_type(Arc::clone(&related_model), Some(Arc::clone(&parent_field)));

        if create_object.into_arc().is_empty() {
            return None;
        }

        let type_name = if parent_field.related_field().is_hidden {
            format!("{}UpsertNestedInput", related_model.name.clone())
        } else {
            format!(
                "{}UpsertWithout{}Input",
                related_model.name.clone(),
                capitalize(parent_field.related_field().name.clone())
            )
        };

        match self.get_cache().get(&type_name) {
            None => {
                let input_object = Arc::new(init_input_object_type(type_name.clone()));
                self.cache(type_name, Arc::clone(&input_object));

                let fields = vec![
                    input_field("update", InputType::object(update_object)),
                    input_field("create", InputType::object(create_object)),
                ];

                input_object.set_fields(fields);
                Some(Arc::downgrade(&input_object))
            }
            x => x,
        }
    }

    /// Builds "deleteMany" field for nested updates (on relation fields).
    fn nested_delete_many_field(&self, field: RelationFieldRef) -> Option<InputField> {
        if field.is_list {
            let input_object = self
                .get_filter_object_builder()
                .scalar_filter_object_type(field.related_model());
            let input_type = InputType::opt(InputType::list(InputType::object(input_object)));

            Some(input_field("deleteMany", input_type))
        } else {
            None
        }
    }

    /// Builds "updateMany" field for nested updates (on relation fields).
    fn nested_update_many_field(&self, field: RelationFieldRef) -> Option<InputField> {
        self.nested_update_many_input_object(field).map(|input_object| {
            let input_type = InputType::opt(InputType::list(InputType::object(input_object)));
            input_field("updateMany", input_type)
        })
    }

    /// Builds "<x>UpdateManyWithWhereNestedInput" input object type.
    fn nested_update_many_input_object(&self, field: RelationFieldRef) -> Option<InputObjectTypeRef> {
        if field.is_list {
            let related_model = field.related_model();
            let type_name = format!("{}UpdateManyWithWhereNestedInput", related_model.name);

            match self.get_cache().get(&type_name) {
                None => {
                    let data_input_object = self.nested_update_many_data(Arc::clone(&field));
                    let input_object = Arc::new(init_input_object_type(type_name.clone()));
                    self.cache(type_name, Arc::clone(&input_object));

                    let where_input_object = self
                        .get_filter_object_builder()
                        .scalar_filter_object_type(related_model);

                    input_object.set_fields(vec![
                        input_field("where", InputType::object(where_input_object)),
                        input_field("data", InputType::object(data_input_object)),
                    ]);

                    Some(Arc::downgrade(&input_object))
                }
                x => return x,
            }
        } else {
            None
        }
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

    /// Builds "<x>UpdateWithWhereUniqueNestedInput" / "<x>UpdateWithWhereUniqueWithout<y>Input" input object types.
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

    /// Builds "<x>UpdateDataInput" / "<x>UpdateWithout<y>DataInput" ubout input object types.
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

    /// Builds "<x>UpdateManyDataInput" input object type.
    fn nested_update_many_data(&self, parent_field: RelationFieldRef) -> InputObjectTypeRef {
        let related_model = parent_field.related_model();
        let type_name = format!("{}UpdateManyDataInput", related_model.name);

        return_cached!(self.get_cache(), &type_name);

        let input_object = Arc::new(init_input_object_type(type_name.clone()));
        self.cache(type_name, Arc::clone(&input_object));

        let fields = self.scalar_input_fields_for_update(Arc::clone(&related_model));

        input_object.set_fields(fields);
        Arc::downgrade(&input_object)
    }
}
