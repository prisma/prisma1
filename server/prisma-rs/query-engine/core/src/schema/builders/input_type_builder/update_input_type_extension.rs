use super::*;
use prisma_models::{ModelRef, RelationFieldRef, ScalarFieldRef};

pub trait UpdateInputTypeBuilderExtension: InputTypeBuilderBase {
    fn update_input_type(&self, model: ModelRef) -> InputObjectTypeRef {
        let name = format!("{}UpdateInput", model.name.clone());
        return_cached!(self.get_cache(), &name);

        let input_object = Arc::new(init_input_object_type(name.clone()));
        self.cache(name, Arc::clone(&input_object));

        // Compute input fields for scalar fields.
        let scalar_fields: Vec<ScalarFieldRef> = model
            .fields()
            .scalar()
            .into_iter()
            .filter(|f| f.is_writable())
            .collect();

        let mut fields = self.scalar_input_fields(model.name.clone(), "Update", scalar_fields, |f: ScalarFieldRef| {
            self.map_optional_input_type(f)
        });

        // Compute input fields for relational fields.
        let mut relational_fields = self.relation_input_fields_update(Arc::clone(&model), None);
        fields.append(&mut relational_fields);

        input_object.set_fields(fields);
        Arc::downgrade(&input_object)
    }

    /// For update input types only. Compute input fields for relational fields.
    /// This recurses into create_input_type (via nested_create_input_field).
    /// Todo: This code is fairly similar to "create" relation computation. Let's see if we can dry it up.
    fn relation_input_fields_update(
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

                            let mut fields = vec![];

                            let nested_connect = self.nested_connect_input_field(Arc::clone(&rf));
                            append_opt(&mut fields, nested_connect);

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

    //     def nestedUpdateInputField(field: RelationField): Option[InputField[Any]] = {
    //     val inputObjectType = computeInputObjectTypeForNestedUpdate(field)
    //     generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("update", x))
    //   }

    fn nested_update_input_field(&self, field: RelationFieldRef) -> InputField {
        let related_model = field.related_model();

        // val subModel = parentField.relatedModel_!
        // computeInputObjectTypeForNestedUpdateData(parentField).flatMap { updateDataInput =>
        //   if (parentField.isList) {
        //     for {
        //       whereArg <- computeInputObjectTypeForWhereUnique(subModel)
        //     } yield {
        //       val typeName = parentField.relatedField.isHidden match {
        //         case false => s"${subModel.name}UpdateWithWhereUniqueWithout${parentField.relatedField.name.capitalize}Input"
        //         case true  => s"${subModel.name}UpdateWithWhereUniqueNestedInput"
        //       }

        //       InputObjectType[Any](
        //         name = typeName,
        //         fieldsFn = () => {
        //           List(
        //             InputField[Any]("where", whereArg),
        //             InputField[Any]("data", updateDataInput)
        //           )
        //         }
        //       )
        //     }
        //   } else {
        //     Some(updateDataInput)
        //   }
        // }
        // let input_object = Self::wrap_list_input_object_type(input_object, field.is_list);

        // input_field("create", input_object)

        unimplemented!()
    }

    fn nested_update_data(&self, field: RelationFieldRef) -> InputField {
        let related_model = field.related_model();
        //     val subModel = parentField.relatedModel_!
        // val fields   = computeScalarInputFieldsForUpdate(subModel) ++ computeRelationalInputFieldsForUpdate(subModel, parentField = Some(parentField))

        // if (fields.nonEmpty) {
        //   val typeName = parentField.relatedField.isHidden match {
        //     case false => s"${subModel.name}UpdateWithout${parentField.relatedField.name.capitalize}DataInput"
        //     case true  => s"${subModel.name}UpdateDataInput"
        //   }

        //   Some(
        //     InputObjectType[Any](
        //       name = typeName,
        //       fieldsFn = () => { fields }
        //     )
        //   )
        // } else {
        //   None
        // }
        unimplemented!()
    }
}
