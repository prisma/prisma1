use crate::{ast, dml};

pub mod directive;

use crate::common::value::ValueValidator;
use crate::dml::fromstr::FromStrAndSpan;
use crate::errors::{ErrorCollection, ValidationError};
use crate::source;
use directive::builtin::{new_builtin_enum_directives, new_builtin_field_directives, new_builtin_model_directives};
use directive::DirectiveListValidator;
use std::collections::HashMap;

/// Helper for validating a datamodel.
///
/// When validating, the
/// AST is converted to the real datamodel, and
/// additional semantics are attached.
pub struct Validator {
    field_directives: DirectiveListValidator<dml::Field>,
    model_directives: DirectiveListValidator<dml::Model>,
    enum_directives: DirectiveListValidator<dml::Enum>,
}

/// State error message. Seeing this error means something went really wrong internally. It's the datamodel equivalent of a bluescreen.
const STATE_ERROR: &str = "Failed lookup of model or field during internal processing. This means that the internal representation was mutated incorrectly.";

impl Validator {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> Validator {
        Validator {
            field_directives: new_builtin_field_directives(),
            model_directives: new_builtin_model_directives(),
            enum_directives: new_builtin_enum_directives(),
        }
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(sources: &Vec<Box<source::Source>>) -> Validator {
        let mut validator = Validator::new();

        for source in sources {
            validator
                .enum_directives
                .add_all_scoped(source.get_enum_directives(), source.name());
            validator
                .field_directives
                .add_all_scoped(source.get_field_directives(), source.name());
            validator
                .model_directives
                .add_all_scoped(source.get_model_directives(), source.name());
        }

        return validator;
    }

    /// Validates an AST semantically and promotes it to a datamodel/schema.
    ///
    /// This method will attempt to
    /// * Resolve all directives
    /// * Recursively evaluate all functions
    /// * Perform string interpolation
    /// * Resolve and check default values
    /// * Resolve and check all field types
    pub fn validate(&self, ast_schema: &ast::Schema) -> Result<dml::Schema, ErrorCollection> {
        // Phase 0 is parsing.
        // Phase 1 is source block loading.

        // Phase 2: Prechecks.
        // TODO: Precheck no duplicate models, fields or directives.
        // TODO: Maybe we move prechecks into different module.

        // Phase 3: Lift AST to DML.
        let mut schema = self.lift(ast_schema)?;

        // Phase 4: Validation
        self.validate_internal(ast_schema, &mut schema)?;

        // Phase 5: Consistency fixes. These don't fail.
        self.make_consistent(&mut schema);

        Ok(schema)
    }

    fn make_consistent(&self, schema: &mut dml::Schema) {
        // Model Consistency. THese ones do not fail.
        // TODO: Also need to hook up the id field with to.
        self.add_missing_back_relations(schema);

        // No normalization of to_fields for now.
        // self.set_relation_to_field_to_id_if_missing(schema);
    }

    fn validate_internal(&self, ast_schema: &ast::Schema, schema: &mut dml::Schema) -> Result<(), ErrorCollection> {
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
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(())
        }
    }

    #[allow(unused)]
    fn validate_model_has_id(&self, ast_model: &ast::Model, model: &dml::Model) -> Result<(), ValidationError> {
        if model.fields().filter(|m| m.id_info.is_some()).count() == 0 {
            Err(ValidationError::new_model_validation_error(
                "One field must be marked as the id field with the `@id` directive.",
                &model.name,
                &ast_model.span,
            ))
        } else {
            Ok(())
        }
    }

    /// Elegantly checks if any relations in the model are ambigious.
    fn validate_relations_not_ambiguous(
        &self,
        ast_schema: &ast::Schema,
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
    #[allow(unused)] // No normalization of to_fields for now.
    fn set_relation_to_field_to_id_if_missing(&self, schema: &mut dml::Schema) {
        // Build up index structure first, because rust does not allow mutatble iteration.
        let mut id_per_model: HashMap<String, Vec<String>> = HashMap::new();

        for model in schema.models() {
            id_per_model.insert(model.name.clone(), model.id_fields().map(|x| x.clone()).collect());
        }

        // Index structure for embedded relations. (HOSTING_MODEL, TARGET_MODEL, NAME) -> (has_embedding, arity)
        let mut relation_has_embedding: HashMap<(String, String, Option<String>), (bool, dml::FieldArity)> =
            HashMap::new();

        for model in schema.models() {
            for field in model.fields() {
                if let dml::FieldType::Relation(rel) = &field.field_type {
                    // Remember if we have an explicit embedding and our arity.
                    relation_has_embedding.insert(
                        (model.name.clone(), rel.to.clone(), rel.name.clone()),
                        (rel.to_fields.len() > 0, field.arity),
                    );
                }
            }
        }

        // Iterate and mutate models.
        for model in schema.models_mut() {
            let model_name = model.name.clone();
            for field in model.fields_mut() {
                if let dml::FieldType::Relation(rel) = &mut field.field_type {
                    // Do we have an embedding, or does our neighbor have an embedding?
                    let (relation_has_embedding, related_exists, related_is_list) =
                        match relation_has_embedding.get(&(rel.to.clone(), model_name.clone(), rel.name.clone())) {
                            Some((has, dml::FieldArity::List)) => (*has, true, true),
                            Some((has, _)) => (*has, true, false),
                            None => (false, false, false),
                        };

                    let we_have_embedding = rel.to_fields.len() > 0;
                    let we_are_list = field.arity == dml::FieldArity::List;

                    // Set to_fields to ID if:
                    // * Embedding is not already set and we are not a list.
                    // * Our related side does exist and has no embedding and our name is smaller
                    // * Our related side does not exist.
                    // * Our related side is a list.
                    if !we_have_embedding
                        && !we_are_list
                        && (!related_exists || related_is_list || (!relation_has_embedding && (model_name < rel.to)))
                    {
                        rel.to_fields = id_per_model.get(&rel.to).expect(STATE_ERROR).clone();
                    }
                }
            }
        }
    }

    /// Identifies and adds missing back relations.
    fn add_missing_back_relations(&self, schema: &mut dml::Schema) {
        let mut missing_back_relations = Vec::new();

        for model in schema.models() {
            missing_back_relations.append(&mut self.find_fields_with_missing_back_relation(model, schema));
        }

        for (forward, backward) in missing_back_relations {
            let model = schema.find_model_mut(&forward.to).expect(STATE_ERROR);

            // TODO: Ugly generated name for now.
            model.add_field(dml::Field::new_generated(
                &format!("generated_{}", backward.to),
                dml::FieldType::Relation(backward),
            ))
        }
    }

    /// Finds all fields which have a missing back relation.
    /// Returns a tuple of (forward_relation, back_relation)
    fn find_fields_with_missing_back_relation(
        &self,
        model: &dml::Model,
        schema: &dml::Schema,
    ) -> Vec<(dml::RelationInfo, dml::RelationInfo)> {
        let mut fields: Vec<(dml::RelationInfo, dml::RelationInfo)> = Vec::new();

        for field in model.fields() {
            if let dml::FieldType::Relation(rel) = &field.field_type {
                let mut back_field_exists = false;

                for back_field in schema.find_model(&rel.to).expect(STATE_ERROR).fields() {
                    if let dml::FieldType::Relation(back_rel) = &back_field.field_type {
                        if back_rel.name == rel.name {
                            back_field_exists = true;
                        }
                    }
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

    pub fn lift(&self, ast_schema: &ast::Schema) -> Result<dml::Schema, ErrorCollection> {
        let mut schema = dml::Schema::new();
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
    fn lift_model(&self, ast_model: &ast::Model, ast_schema: &ast::Schema) -> Result<dml::Model, ErrorCollection> {
        let mut model = dml::Model::new(&ast_model.name);
        let mut errors = ErrorCollection::new();

        for ast_field in &ast_model.fields {
            match self.lift_field(ast_field, ast_schema) {
                Ok(field) => model.add_field(field),
                Err(mut err) => errors.append(&mut err),
            }
        }

        if let Err(mut err) = self.model_directives.validate_and_apply(ast_model, &mut model) {
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

        if let Err(mut err) = self.enum_directives.validate_and_apply(ast_enum, &mut en) {
            errors.append(&mut err);
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(en)
        }
    }

    /// Internal: Lift a field AST node to a DML field.
    fn lift_field(&self, ast_field: &ast::Field, ast_schema: &ast::Schema) -> Result<dml::Field, ErrorCollection> {
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

        if let Err(mut err) = self.field_directives.validate_and_apply(ast_field, &mut field) {
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
        ast_schema: &ast::Schema,
    ) -> Result<dml::FieldType, ValidationError> {
        let type_name = &ast_field.field_type;

        if let Ok(scalar_type) = dml::ScalarType::from_str_and_span(type_name, span) {
            Ok(dml::FieldType::Base(scalar_type))
        } else {
            if let Some(_) = ast_schema.find_model(type_name) {
                if let Some(to_field) = &ast_field.field_link {
                    return Ok(dml::FieldType::Relation(dml::RelationInfo::new_with_fields(
                        &ast_field.field_type,
                        vec![to_field.clone()],
                    )));
                } else {
                    Ok(dml::FieldType::Relation(dml::RelationInfo::new(&ast_field.field_type)))
                }
            } else if let Some(_) = ast_schema.find_enum(type_name) {
                Ok(dml::FieldType::Enum(type_name.clone()))
            } else {
                Err(ValidationError::new_type_not_found_error(type_name, span))
            }
        }
    }
}

trait FindInAstSchema {
    fn find_field(&self, model: &str, field: &str) -> Option<&ast::Field>;
    fn find_model(&self, model: &str) -> Option<&ast::Model>;
    fn find_enum(&self, enum_name: &str) -> Option<&ast::Enum>;
}

impl FindInAstSchema for ast::Schema {
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
