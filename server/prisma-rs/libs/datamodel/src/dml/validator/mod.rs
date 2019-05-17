use crate::{ast, dml};

pub mod argument;
pub mod directive;
pub mod value;

use crate::errors::{ErrorCollection, ValidationError};
use directive::builtin::{
    new_builtin_enum_directives, new_builtin_field_directives, new_builtin_model_directives, DirectiveListValidator,
};
use value::ValueValidator;

pub trait DirectiveSource<T> {
    fn get_directives(validator: &mut DirectiveListValidator<T>);
}

// TODO: Naming
pub struct Validator {
    field_directives: DirectiveListValidator<dml::Field>,
    model_directives: DirectiveListValidator<dml::Model>,
    enum_directives: DirectiveListValidator<dml::Enum>,
}

impl Validator {
    pub fn new() -> Self {
        Validator {
            field_directives: new_builtin_field_directives(),
            model_directives: new_builtin_model_directives(),
            enum_directives: new_builtin_enum_directives(),
        }
    }

    pub fn validate(&self, ast_schema: &ast::Schema) -> Result<dml::Schema, ErrorCollection> {
        let mut schema = dml::Schema::new();
        let mut errors = ErrorCollection::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::ModelOrEnum::Enum(en) => match self.validate_enum(&en) {
                    Ok(en) => schema.add_enum(en),
                    Err(mut err) => errors.append(&mut err),
                },
                ast::ModelOrEnum::Model(ty) => match self.validate_model(&ty, ast_schema) {
                    Ok(md) => schema.add_model(md),
                    Err(mut err) => errors.append(&mut err),
                },
            }
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(schema)
        }
    }

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

    fn validate_field(&self, ast_field: &ast::Field, ast_schema: &ast::Schema) -> Result<dml::Field, ErrorCollection> {
        let mut errors = ErrorCollection::new();
        // If we cannot parse the field type, we exit right away.
        let field_type = self.validate_field_type(&ast_field.field_type, &ast_field.field_type_span, ast_schema)?;

        let mut field = dml::Field::new(&ast_field.name, field_type.clone());

        field.arity = self.validate_field_arity(&ast_field.arity);

        if let Some(value) = &ast_field.default_value {
            if let dml::FieldType::Base(base_type) = &field_type {
                match (ValueValidator { value: value.clone() }).as_type(base_type) {
                    Ok(val) => field.default_value = Some(val),
                    Err(err) => errors.push(err),
                };
            } else {
                errors.push(ValidationError::new_parser_error(
                    "Found default value for a non-scalar type.",
                    ValueValidator { value: value.clone() }.span(),
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

    fn validate_field_arity(&self, ast_field: &ast::FieldArity) -> dml::FieldArity {
        match ast_field {
            ast::FieldArity::Required => dml::FieldArity::Required,
            ast::FieldArity::Optional => dml::FieldArity::Optional,
            ast::FieldArity::List => dml::FieldArity::List,
        }
    }

    fn validate_field_type(
        &self,
        type_name: &str,
        span: &ast::Span,
        ast_schema: &ast::Schema,
    ) -> Result<dml::FieldType, ValidationError> {
        match type_name {
            "ID" => Ok(dml::FieldType::Base(dml::ScalarType::Int)),
            "Int" => Ok(dml::FieldType::Base(dml::ScalarType::Int)),
            "Float" => Ok(dml::FieldType::Base(dml::ScalarType::Float)),
            "Decimal" => Ok(dml::FieldType::Base(dml::ScalarType::Decimal)),
            "Boolean" => Ok(dml::FieldType::Base(dml::ScalarType::Boolean)),
            "String" => Ok(dml::FieldType::Base(dml::ScalarType::String)),
            "DateTime" => Ok(dml::FieldType::Base(dml::ScalarType::DateTime)),
            // Distinguish between relation and enum.
            _ => {
                for model in &ast_schema.models {
                    match &model {
                        // TODO: Get primary key field and hook up String::from.
                        ast::ModelOrEnum::Model(model) if model.name == *type_name => {
                            return Ok(dml::FieldType::Relation(dml::RelationInfo::new(&type_name)))
                        }
                        ast::ModelOrEnum::Enum(en) if en.name == *type_name => {
                            return Ok(dml::FieldType::Enum(String::from(type_name)))
                        }
                        _ => {}
                    }
                }
                Err(ValidationError::new_type_not_found_error(type_name, span))
            }
        }
    }
}
