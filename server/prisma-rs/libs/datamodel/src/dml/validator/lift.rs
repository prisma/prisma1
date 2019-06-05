use super::{common::*, DirectiveBox};
use crate::{
    ast,
    common::value::ValueValidator,
    common::{FromStrAndSpan, PrismaType},
    dml,
    errors::{ErrorCollection, ValidationError},
    source,
};

/// Helper for lifting a datamodel.
///
/// When lifting, the
/// AST is converted to the real datamodel, and
/// additional semantics are attached.
pub struct LiftAstToDml {
    directives: DirectiveBox,
}

impl LiftAstToDml {
    /// Creates a new instance, with all builtin directives registered.
    pub fn new() -> LiftAstToDml {
        LiftAstToDml {
            directives: DirectiveBox::new(),
        }
    }

    /// Creates a new instance, with all builtin directives and
    /// the directives defined by the given sources registered.
    ///
    /// The directives defined by the given sources will be namespaced.
    pub fn with_sources(sources: &Vec<Box<source::Source>>) -> LiftAstToDml {
        LiftAstToDml {
            directives: DirectiveBox::with_sources(sources),
        }
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
                errors.push(ValidationError::new_validation_error(
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

        if let Ok(scalar_type) = PrismaType::from_str_and_span(type_name, span) {
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