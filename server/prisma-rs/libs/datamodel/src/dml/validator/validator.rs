use crate::{ast, dml};

use super::DirectiveBox;
use crate::common::{names::*, value::ValueValidator};
use crate::dml::fromstr::FromStrAndSpan;
use crate::errors::{ErrorCollection, ValidationError};
use crate::source;

/// Helper for validating a datamodel.
///
/// When validating, the
/// AST is converted to the real datamodel, and
/// additional semantics are attached.
pub struct Validator {
    directives: DirectiveBox,
}

/// State error message. Seeing this error means something went really wrong internally. It's the datamodel equivalent of a bluescreen.
const STATE_ERROR: &str = "Failed lookup of model or field during internal processing. This means that the internal representation was mutated incorrectly.";

impl Validator {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> Validator {
        Validator {
            directives: DirectiveBox::new(),
        }
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(sources: &Vec<Box<source::Source>>) -> Validator {
        Validator {
            directives: DirectiveBox::with_sources(sources),
        }
    }

    /// Validates an AST semantically and promotes it to a datamodel/schema.
    ///
    /// This method will attempt to
    /// * Resolve all directives
    /// * Recursively evaluate all functions
    /// * Perform string interpolation
    /// * Resolve and check default values
    /// * Resolve and check all field types
    pub fn validate(&self, ast_schema: &ast::Datamodel) -> Result<dml::Datamodel, ErrorCollection> {
        // Phase 0 is parsing.
        // Phase 1 is source block loading.

        // Phase 2: Prechecks.
        // TODO: Precheck no duplicate models, fields or directives.
        // TODO: Maybe we move prechecks into different module.

        // Phase 3: Lift AST to DML.
        let mut schema = self.lift(ast_schema)?;

        // Phase 4: Validation
        self.validate_internal(ast_schema, &mut schema)?;

        // TODO: Move consistency stuff into different module.
        // Phase 5: Consistency fixes. These don't fail.
        self.make_consistent(ast_schema, &mut schema)?;

        Ok(schema)
    }

    fn make_consistent(&self, ast_schema: &ast::Datamodel, schema: &mut dml::Datamodel) -> Result<(), ErrorCollection> {
        // Model Consistency. These ones do not fail.
        // TODO: Also need to hook up the id field with to.
        self.add_missing_back_relations(ast_schema, schema)?;

        // Always give relations a to_field.
        self.set_relation_to_field_to_id_if_missing(schema);

        // Always give relations some name.
        self.name_unnamed_relations(schema);

        Ok(())
    }

    fn validate_internal(
        &self,
        ast_schema: &ast::Datamodel,
        schema: &mut dml::Datamodel,
    ) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();

        // Model level validations.
        for model in schema.models() {
            if let Err(err) = self.validate_model_has_id(ast_schema.find_model(&model.name).expect(STATE_ERROR), model)
            {
                errors.push(err);
            }
            if let Err(err) = self.validate_relations_not_ambiguous(ast_schema, model) {
                errors.push(err);
            }
            if let Err(err) = self.validate_embedded_types_have_no_back_relation(ast_schema, schema, model) {
                errors.push(err);
            }
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(())
        }
    }

    #[allow(unused)]
    fn validate_model_has_id(&self, ast_model: &ast::Model, model: &dml::Model) -> Result<(), ValidationError> {
        let related_fields = model.fields().filter(|f| -> bool {
            if let dml::FieldType::Relation(_) = f.field_type {
                f.arity != dml::FieldArity::List
            } else {
                false
            }
        });

        if related_fields.count() == 2 {
            return Ok(());
            // Extempt from the id rule, we have an relation table.
        }

        if model.id_fields().count() == 0 {
            Err(ValidationError::new_model_validation_error(
                "One field must be marked as the id field with the `@id` directive.",
                &model.name,
                &ast_model.span,
            ))
        } else {
            Ok(())
        }
    }

    /// Ensures that embedded types do not have back relations
    /// to their parent types.
    fn validate_embedded_types_have_no_back_relation(
        &self,
        ast_schema: &ast::Datamodel,
        datamodel: &dml::Datamodel,
        model: &dml::Model,
    ) -> Result<(), ValidationError> {
        if model.is_embedded {
            for field in model.fields() {
                if !field.is_generated {
                    if let dml::FieldType::Relation(rel) = &field.field_type {
                        // TODO: I am not sure if this check is d'accord with the query engine.
                        let related = datamodel.find_model(&rel.to).unwrap();
                        let related_field = related.related_field(&model.name, &rel.name, &field.name).unwrap();
                        if rel.to_fields.len() == 0 && !related_field.is_generated {
                            // TODO: Refactor that out, it's way too much boilerplate.
                            return Err(ValidationError::new_model_validation_error(
                                "Embedded models cannot have back relation fields.",
                                &model.name,
                                &ast_schema.find_field(&model.name, &field.name).expect(STATE_ERROR).span,
                            ));
                        }
                    }
                }
            }
        }

        Ok(())
    }

    /// Elegantly checks if any relations in the model are ambigious.
    fn validate_relations_not_ambiguous(
        &self,
        ast_schema: &ast::Datamodel,
        model: &dml::Model,
    ) -> Result<(), ValidationError> {
        for field_a in model.fields() {
            for field_b in model.fields() {
                if field_a != field_b {
                    if let dml::FieldType::Relation(rel_a) = &field_a.field_type {
                        if let dml::FieldType::Relation(rel_b) = &field_b.field_type {
                            if rel_a.to != model.name && rel_b.to != model.name {
                                // Not a self relation
                                // but pointing to the same foreign model,
                                // and also no names set.
                                if rel_a.to == rel_b.to && rel_a.name == rel_b.name {
                                    return Err(ValidationError::new_model_validation_error(
                                        "Ambiguous relation detected.",
                                        &model.name,
                                        &ast_schema
                                            .find_field(&model.name, &field_a.name)
                                            .expect(STATE_ERROR)
                                            .span,
                                    ));
                                }
                            } else {
                                // A self relation...
                                for field_c in model.fields() {
                                    if field_a != field_c && field_b != field_c {
                                        if let dml::FieldType::Relation(rel_c) = &field_c.field_type {
                                            // ...but there are more thatn three fields without a name.
                                            if rel_c.to == model.name
                                                && rel_a.name == rel_b.name
                                                && rel_a.name == rel_c.name
                                            {
                                                return Err(ValidationError::new_model_validation_error(
                                                    "Ambiguous self relation detected.",
                                                    &model.name,
                                                    &ast_schema
                                                        .find_field(&model.name, &field_a.name)
                                                        .expect(STATE_ERROR)
                                                        .span,
                                                ));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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

    pub fn name_unnamed_relations(&self, datamodel: &mut dml::Datamodel) {
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

    pub fn lift(&self, ast_schema: &ast::Datamodel) -> Result<dml::Datamodel, ErrorCollection> {
        let mut schema = dml::Datamodel::new();
        let mut errors = ErrorCollection::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::Top::Enum(en) => match self.lift_enum(&en) {
                    Ok(en) => schema.add_enum(en),
                    Err(mut err) => errors.append(&mut err),
                },
                ast::Top::Model(ty) => match self.lift_model(&ty, ast_schema) {
                    Ok(md) => schema.add_model(md),
                    Err(mut err) => errors.append(&mut err),
                },
                ast::Top::Source(_) => { /* Source blocks are explicitely ignored by the validator */ }
            }
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(schema)
        }
    }

    /// Internal: Validates a model AST node and lifts it to a DML model.
    fn lift_model(&self, ast_model: &ast::Model, ast_schema: &ast::Datamodel) -> Result<dml::Model, ErrorCollection> {
        let mut model = dml::Model::new(&ast_model.name);
        let mut errors = ErrorCollection::new();

        for ast_field in &ast_model.fields {
            match self.lift_field(ast_field, ast_schema) {
                Ok(field) => model.add_field(field),
                Err(mut err) => errors.append(&mut err),
            }
        }

        if let Err(mut err) = self.directives.model.validate_and_apply(ast_model, &mut model) {
            errors.append(&mut err);
        }

        if errors.has_errors() {
            return Err(errors);
        }

        Ok(model)
    }

    /// Internal: Validates an enum AST node.
    fn lift_enum(&self, ast_enum: &ast::Enum) -> Result<dml::Enum, ErrorCollection> {
        let mut en = dml::Enum::new(&ast_enum.name, ast_enum.values.clone());
        let mut errors = ErrorCollection::new();

        if let Err(mut err) = self.directives.enm.validate_and_apply(ast_enum, &mut en) {
            errors.append(&mut err);
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(en)
        }
    }

    /// Internal: Lift a field AST node to a DML field.
    fn lift_field(&self, ast_field: &ast::Field, ast_schema: &ast::Datamodel) -> Result<dml::Field, ErrorCollection> {
        let mut errors = ErrorCollection::new();
        // If we cannot parse the field type, we exit right away.
        let field_type = self.lift_field_type(&ast_field, &ast_field.field_type_span, ast_schema)?;

        let mut field = dml::Field::new(&ast_field.name, field_type.clone());

        field.arity = self.lift_field_arity(&ast_field.arity);

        if let Some(value) = &ast_field.default_value {
            let validator = ValueValidator::new(value)?;
            if let dml::FieldType::Base(base_type) = &field_type {
                match validator.as_type(base_type) {
                    Ok(val) => field.default_value = Some(val),
                    Err(err) => errors.push(err),
                };
            } else {
                errors.push(ValidationError::new_parser_error(
                    "Found default value for a non-scalar type.",
                    validator.span(),
                ))
            }
        }

        if let Err(mut err) = self.directives.field.validate_and_apply(ast_field, &mut field) {
            errors.append(&mut err);
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(field)
        }
    }

    /// Internal: Lift a field's arity.
    fn lift_field_arity(&self, ast_field: &ast::FieldArity) -> dml::FieldArity {
        match ast_field {
            ast::FieldArity::Required => dml::FieldArity::Required,
            ast::FieldArity::Optional => dml::FieldArity::Optional,
            ast::FieldArity::List => dml::FieldArity::List,
        }
    }

    /// Internal: Lift a field's type.
    fn lift_field_type(
        &self,
        ast_field: &ast::Field,
        span: &ast::Span,
        ast_schema: &ast::Datamodel,
    ) -> Result<dml::FieldType, ValidationError> {
        let type_name = &ast_field.field_type;

        if let Ok(scalar_type) = dml::ScalarType::from_str_and_span(type_name, span) {
            Ok(dml::FieldType::Base(scalar_type))
        } else {
            if let Some(_) = ast_schema.find_model(type_name) {
                Ok(dml::FieldType::Relation(dml::RelationInfo::new(&ast_field.field_type)))
            } else if let Some(_) = ast_schema.find_enum(type_name) {
                Ok(dml::FieldType::Enum(type_name.clone()))
            } else {
                Err(ValidationError::new_type_not_found_error(type_name, span))
            }
        }
    }
}

trait FindInAstDatamodel {
    fn find_field(&self, model: &str, field: &str) -> Option<&ast::Field>;
    fn find_model(&self, model: &str) -> Option<&ast::Model>;
    fn find_enum(&self, enum_name: &str) -> Option<&ast::Enum>;
}

impl FindInAstDatamodel for ast::Datamodel {
    fn find_field(&self, model: &str, field: &str) -> Option<&ast::Field> {
        for ast_field in &self.find_model(model)?.fields {
            if ast_field.name == field {
                return Some(&ast_field);
            }
        }

        None
    }

    fn find_model(&self, model: &str) -> Option<&ast::Model> {
        for ast_top in &self.models {
            if let ast::Top::Model(ast_model) = ast_top {
                if ast_model.name == model {
                    return Some(&ast_model);
                }
            }
        }

        None
    }

    fn find_enum(&self, enum_name: &str) -> Option<&ast::Enum> {
        for ast_top in &self.models {
            if let ast::Top::Enum(ast_enum) = ast_top {
                if ast_enum.name == enum_name {
                    return Some(&ast_enum);
                }
            }
        }

        None
    }
}
