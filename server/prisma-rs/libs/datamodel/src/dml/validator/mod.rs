use crate::{ast, dml};

pub mod directive;

use crate::common::value::ValueValidator;
use crate::dml::fromstr::FromStrAndSpan;
use crate::errors::{ErrorCollection, ValidationError};
use crate::source;
use directive::builtin::{new_builtin_enum_directives, new_builtin_field_directives, new_builtin_model_directives};
use directive::DirectiveListValidator;

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
        let mut schema = dml::Schema::new();
        let mut errors = ErrorCollection::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::Top::Enum(en) => match self.validate_enum(&en) {
                    Ok(en) => schema.add_enum(en),
                    Err(mut err) => errors.append(&mut err),
                },
                ast::Top::Model(ty) => match self.validate_model(&ty, ast_schema) {
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

    /// Internal: Validates a model AST node.
    fn validate_model(&self, ast_model: &ast::Model, ast_schema: &ast::Schema) -> Result<dml::Model, ErrorCollection> {
        let mut model = dml::Model::new(&ast_model.name);
        let mut errors = ErrorCollection::new();

        for ast_field in &ast_model.fields {
            match self.validate_field(ast_field, ast_schema) {
                Ok(field) => model.add_field(field),
                Err(mut err) => errors.append(&mut err),
            }
        }

        if let Err(mut err) = self.model_directives.validate_and_apply(ast_model, &mut model) {
            errors.append(&mut err);
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(model)
        }
    }

    /// Internal: Validates an enum AST node.
    fn validate_enum(&self, ast_enum: &ast::Enum) -> Result<dml::Enum, ErrorCollection> {
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

    /// Internal: Validates a field AST node.
    fn validate_field(&self, ast_field: &ast::Field, ast_schema: &ast::Schema) -> Result<dml::Field, ErrorCollection> {
        let mut errors = ErrorCollection::new();
        // If we cannot parse the field type, we exit right away.
        let field_type = self.validate_field_type(&ast_field.field_type, &ast_field.field_type_span, ast_schema)?;

        let mut field = dml::Field::new(&ast_field.name, field_type.clone());

        field.arity = self.validate_field_arity(&ast_field.arity);

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

    /// Internal: Validates a field's arity.
    fn validate_field_arity(&self, ast_field: &ast::FieldArity) -> dml::FieldArity {
        match ast_field {
            ast::FieldArity::Required => dml::FieldArity::Required,
            ast::FieldArity::Optional => dml::FieldArity::Optional,
            ast::FieldArity::List => dml::FieldArity::List,
        }
    }

    /// Internal: Validates a field's type.
    fn validate_field_type(
        &self,
        type_name: &str,
        span: &ast::Span,
        ast_schema: &ast::Schema,
    ) -> Result<dml::FieldType, ValidationError> {
        if let Ok(scalar_type) = dml::ScalarType::from_str_and_span(type_name, span) {
            Ok(dml::FieldType::Base(scalar_type))
        } else {
            // Distinguish between relation and enum.
            for model in &ast_schema.models {
                match &model {
                    // TODO: Get primary key field and hook up String::from.
                    ast::Top::Model(model) if model.name == *type_name => {
                        return Ok(dml::FieldType::Relation(dml::RelationInfo::new(&type_name)))
                    }
                    ast::Top::Enum(en) if en.name == *type_name => {
                        return Ok(dml::FieldType::Enum(String::from(type_name)))
                    }
                    _ => {}
                }
            }
            Err(ValidationError::new_type_not_found_error(type_name, span))
        }
    }
}
