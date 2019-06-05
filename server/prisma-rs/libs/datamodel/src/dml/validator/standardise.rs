use super::common::*;
use crate::{
    ast,
    common::names::*,
    dml,
    errors::{ErrorCollection, ValidationError},
    source,
};

/// Helper for standardsing a datamodel.
///
/// When standardsing, datamodel will be made consistent.
/// Implicit back relation fields, relation names and `to_fields` will be generated.
pub struct Standardiser {}

impl Standardiser {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> Standardiser {
        Standardiser {}
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(_sources: &Vec<Box<source::Source>>) -> Standardiser {
        Standardiser {}
    }

    pub fn standardise(&self, ast_schema: &ast::Datamodel, schema: &mut dml::Datamodel) -> Result<(), ErrorCollection> {
        // Model Consistency. These ones do not fail.
        // TODO: Also need to hook up the id field with to.
        self.add_missing_back_relations(ast_schema, schema)?;

        // Always give relations a to_field.
        self.set_relation_to_field_to_id_if_missing(schema);

        // Always give relations some name.
        self.name_unnamed_relations(schema);

        Ok(())
    }

    /// For any relations which are missing to_fields, sets them to the @id fields
    /// of the foreign model.
    fn set_relation_to_field_to_id_if_missing(&self, schema: &mut dml::Datamodel) {
        // TODO: This is such a bad solution. :(
        let schema_copy = schema.clone();

        // Iterate and mutate models.
        for model_idx in 0..schema.models.len() {
            let model = &mut schema.models[model_idx];
            let model_name = &model.name;
            for field_index in 0..model.fields.len() {
                let field = &mut model.fields[field_index];

                if let dml::FieldType::Relation(rel) = &mut field.field_type {
                    let related_model = schema_copy.find_model(&rel.to).expect(STATE_ERROR);

                    let related_field = related_model.related_field(model_name, &rel.name, &field.name);

                    let we_have_embedding = rel.to_fields.len() > 0;
                    let we_are_list = field.arity == dml::FieldArity::List;

                    let mut embed_here = false;

                    // If to_fields are already set or this is a list,
                    // we continue.
                    if we_have_embedding || we_are_list {
                        continue;
                    }

                    if related_field.is_none() {
                        // If there is no related field, we always embed.
                        embed_here = true;
                    } else {
                        let related_field = related_field.unwrap();
                        let rel = if let dml::FieldType::Relation(rel) = &related_field.field_type {
                            rel
                        } else {
                            panic!(STATE_ERROR)
                        };

                        // If the related field has to_bields set, we continue.
                        if rel.to_fields.len() > 0 {
                            continue;
                        }

                        // Likewise, if this field is generated, and the related one is not a list,
                        // we continue.
                        if field.is_generated && related_field.arity != dml::FieldArity::List {
                            continue;
                        }

                        // Otherise, we embed if...

                        // ... the related field is a list ...
                        if related_field.arity == dml::FieldArity::List {
                            embed_here = true;
                        }

                        // .. or the related field is not a list, but generated ...
                        if related_field.arity != dml::FieldArity::List && related_field.is_generated {
                            embed_here = true;
                        }

                        // .. tie breaker if both are good candiates.
                        if model_name < &rel.to || (model_name == &rel.to && field.name < related_field.name) {
                            embed_here = true;
                        }
                    }

                    if embed_here {
                        rel.to_fields = related_model.id_fields().map(|x| x.clone()).collect()
                    }
                }
            }
        }
    }

    /// Identifies and adds missing back relations.
    fn add_missing_back_relations(
        &self,
        ast_schema: &ast::Datamodel,
        schema: &mut dml::Datamodel,
    ) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();
        let mut missing_back_relations = Vec::new();

        for model in schema.models() {
            missing_back_relations.append(&mut self.find_fields_with_missing_back_relation(model, schema));
        }

        for (forward, backward) in missing_back_relations {
            let model = schema.find_model_mut(&forward.to).expect(STATE_ERROR);

            let name = backward.to.camel_case();

            if let Some(conflicting_field) = model.find_field(&name) {
                println!("Error adding field");
                errors.push(ValidationError::new_model_validation_error(
                    "Automatic back field generation would cause a naming conflict.",
                    &model.name,
                    &ast_schema
                        .find_field(&model.name, &conflicting_field.name)
                        .expect(STATE_ERROR)
                        .span,
                ));
            }

            model.add_field(dml::Field::new_generated(&name, dml::FieldType::Relation(backward)));
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(())
        }
    }

    /// Finds all fields which have a missing back relation.
    /// Returns a tuple of (forward_relation, back_relation)
    fn find_fields_with_missing_back_relation(
        &self,
        model: &dml::Model,
        schema: &dml::Datamodel,
    ) -> Vec<(dml::RelationInfo, dml::RelationInfo)> {
        let mut fields: Vec<(dml::RelationInfo, dml::RelationInfo)> = Vec::new();

        for field in model.fields() {
            if let dml::FieldType::Relation(rel) = &field.field_type {
                let mut back_field_exists = false;

                let related_model = schema.find_model(&rel.to).expect(STATE_ERROR);
                if related_model
                    .related_field(&model.name, &rel.name, &field.name)
                    .is_some()
                {
                    back_field_exists = true;
                }

                if !back_field_exists {
                    fields.push((
                        // Forward
                        rel.clone(),
                        // Backward
                        dml::RelationInfo {
                            to: model.name.clone(),
                            to_fields: vec![],
                            name: rel.name.clone(),
                            on_delete: rel.on_delete,
                        },
                    ));
                }
            }
        }

        fields
    }

    fn name_unnamed_relations(&self, datamodel: &mut dml::Datamodel) {
        let unnamed_relations = self.find_unnamed_relations(&datamodel);

        for (model_name, field_name, rel_info) in unnamed_relations {
            // Embedding side.
            let field = datamodel
                .find_model_mut(&model_name)
                .expect(STATE_ERROR)
                .find_field_mut(&field_name)
                .expect(STATE_ERROR);

            if let dml::FieldType::Relation(rel) = &mut field.field_type {
                rel.name = DefaultNames::relation_name(&model_name, &rel_info.to);
            } else {
                panic!("Tried to name a non-existing relation.");
            }

            // Foreign site.
            let field = datamodel
                .find_model_mut(&rel_info.to)
                .expect(STATE_ERROR)
                .related_field_mut(&model_name, &rel_info.name, &field_name)
                .expect(STATE_ERROR);

            if let dml::FieldType::Relation(rel) = &mut field.field_type {
                rel.name = DefaultNames::relation_name(&model_name, &rel_info.to);
            } else {
                panic!("Tried to name a non-existing relation.");
            }
        }
    }

    // Returns list of model name, field name and relation info.
    fn find_unnamed_relations(&self, datamodel: &dml::Datamodel) -> Vec<(String, String, dml::RelationInfo)> {
        let mut rels = Vec::new();

        for model in datamodel.models() {
            for field in model.fields() {
                if let dml::FieldType::Relation(rel) = &field.field_type {
                    if rel.name.len() == 0 && rel.to_fields.len() > 0 {
                        rels.push((model.name.clone(), field.name.clone(), rel.clone()))
                    }
                }
            }
        }

        rels
    }
}
