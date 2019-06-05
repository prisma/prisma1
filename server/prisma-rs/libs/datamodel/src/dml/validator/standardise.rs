use super::common::*;
use crate::{ast, common::names::*, dml, errors::ErrorCollection, source};

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
        self.add_missing_back_relations(ast_schema, schema)?;

        self.add_missing_relation_tables(ast_schema, schema)?;

        self.set_relation_to_field_to_id_if_missing(schema);

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

    // Rel name, from field, to field.
    fn identify_missing_relation_tables(
        &self,
        schema: &mut dml::Datamodel,
    ) -> Vec<(String, dml::FieldRef, dml::FieldRef)> {
        let mut res = vec![];

        for model in schema.models() {
            for field in model.fields() {
                if field.arity == dml::FieldArity::List {
                    if let dml::FieldType::Relation(rel) = &field.field_type {
                        let related_model = schema.find_model(&rel.to).expect(STATE_ERROR);
                        let related_field = related_model
                            .related_field(&model.name, &rel.name, &field.name)
                            .expect(STATE_ERROR);

                        // Model names, field names are again used as a tie breaker.
                        if related_field.arity == dml::FieldArity::List
                            && (model.name < related_model.name
                                || (model.name == related_model.name && field.name < related_field.name))
                        {
                            // N:M Relation, needs a relation table.
                            res.push((
                                rel.name.clone(),
                                (model.name.clone(), field.name.clone()),
                                (related_model.name.clone(), related_field.name.clone()),
                            ));
                        }
                    }
                }
            }
        }

        res
    }

    fn create_relation_table(
        &self,
        a: &dml::FieldRef,
        b: &dml::FieldRef,
        override_relation_name: &str,
        datamodel: &dml::Datamodel,
    ) -> dml::Model {
        // A vs B tie breaking is done in identify_missing_relation_tables.
        let a_model = datamodel.find_model(&a.0).expect(STATE_ERROR);
        let b_model = datamodel.find_model(&b.0).expect(STATE_ERROR);

        let relation_name = if override_relation_name != "" {
            String::from(override_relation_name)
        } else {
            DefaultNames::relation_name(&a_model.name, &b_model.name)
        };

        let mut a_related_field = self.create_reference_field_for_model(a_model, &relation_name);
        a_related_field.arity = dml::FieldArity::Required;
        let mut b_related_field = self.create_reference_field_for_model(b_model, &relation_name);
        b_related_field.arity = dml::FieldArity::Required;

        dml::Model {
            comments: vec![],
            name: relation_name,
            database_name: None,
            is_embedded: false,
            fields: vec![a_related_field, b_related_field],
            is_generated: true,
        }
    }

    fn create_reference_field_for_model(&self, model: &dml::Model, relation_name: &str) -> dml::Field {
        dml::Field::new_generated(
            &NameNormalizer::camel_case(&model.name),
            dml::FieldType::Relation(dml::RelationInfo {
                to: model.name.clone(),
                to_fields: model.id_fields().map(|s| s.clone()).collect(),
                name: String::from(relation_name), // Will be corrected in later step
                on_delete: dml::OnDeleteStrategy::None,
            }),
        )
    }

    fn point_relation_to(&self, field_ref: &dml::FieldRef, to: &str, datamodel: &mut dml::Datamodel) {
        let field = datamodel.find_field_mut(field_ref).expect(STATE_ERROR);

        if let dml::FieldType::Relation(rel) = &mut field.field_type {
            rel.to = String::from(to);
            rel.to_fields = vec![];
        } else {
            panic!(STATE_ERROR);
        }
    }

    fn add_missing_relation_tables(
        &self,
        ast_schema: &ast::Datamodel,
        schema: &mut dml::Datamodel,
    ) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();

        let all_missing = self.identify_missing_relation_tables(schema);

        for missing in all_missing {
            let rel_table = self.create_relation_table(&missing.1, &missing.2, &missing.0, schema);
            if let Some(conflicting_model) = schema.find_model(&rel_table.name) {
                errors.push(model_validation_error(
                    "Automatic relation table generation would cause a naming conflict.",
                    &conflicting_model,
                    &ast_schema,
                ));
            }
            // TODO: Relation name WILL clash if there is a N:M self relation.
            self.point_relation_to(&missing.1, &rel_table.name, schema);
            self.point_relation_to(&missing.2, &rel_table.name, schema);

            schema.add_model(rel_table);
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(())
        }
    }

    /// Identifies and adds missing back relations. For 1:1 and 1:N relations.
    /// Explicit n:m relations are not touched, as they already have a back relation field.
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
                errors.push(field_validation_error(
                    "Automatic back field generation would cause a naming conflict.",
                    &model,
                    &conflicting_field,
                    &ast_schema,
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
