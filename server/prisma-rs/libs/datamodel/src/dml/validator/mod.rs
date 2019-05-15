use crate::{ast, dml};

pub mod argument;
pub mod directive;
pub mod value;

use crate::errors::DirectiveValidationError;
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

    pub fn validate(&self, ast_schema: &ast::Schema) -> Result<dml::Schema, Vec<DirectiveValidationError>> {
        let mut schema = dml::Schema::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::ModelOrEnum::Enum(en) => schema.add_enum(self.validate_enum(&en)?),
                ast::ModelOrEnum::Model(ty) => schema.add_model(self.validate_model(&ty, ast_schema)?),
            }
        }

        return Ok(schema);
    }

    fn validate_model(
        &self,
        ast_model: &ast::Model,
        ast_schema: &ast::Schema,
    ) -> Result<dml::Model, Vec<DirectiveValidationError>> {
        let mut model = dml::Model::new(&ast_model.name);

        for ast_field in &ast_model.fields {
            model.add_field(self.validate_field(ast_field, ast_schema)?);
        }

        let errs = self.model_directives.validate_and_apply(ast_model, &mut model);

        if errs.len() > 0 {
            return Err(errs);
        }

        return Ok(model);
    }

    fn validate_enum(&self, ast_enum: &ast::Enum) -> Result<dml::Enum, Vec<DirectiveValidationError>> {
        let mut en = dml::Enum::new(&ast_enum.name, ast_enum.values.clone());

        let errs = self.enum_directives.validate_and_apply(ast_enum, &mut en);

        if errs.len() > 0 {
            return Err(errs);
        }

        return Ok(en);
    }

    fn validate_field(
        &self,
        ast_field: &ast::Field,
        ast_schema: &ast::Schema,
    ) -> Result<dml::Field, Vec<DirectiveValidationError>> {
        let field_type = self.validate_field_type(&ast_field.field_type, &ast_field.span, ast_schema)?;

        let mut field = dml::Field::new(&ast_field.name, field_type.clone());

        field.arity = self.validate_field_arity(&ast_field.arity);

        if let Some(value) = &ast_field.default_value {
            if let dml::FieldType::Base(base_type) = &field_type {
                // TODO: Proper error handling.
                // TODO: WrappedValue is not the tool of choice here,
                // there should be a static func for converting stuff.
                field.default_value = Some(
                    (ValueValidator { value: value.clone() })
                        .as_type(base_type)
                        .expect("Unable to parse."),
                );
            } else {
                unimplemented!("Found a default value for a non-scalar type.")
            }
        }

        let errs = self.field_directives.validate_and_apply(ast_field, &mut field);

        if errs.len() > 0 {
            return Err(errs);
        }

        return Ok(field);
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
    ) -> Result<dml::FieldType, Vec<DirectiveValidationError>> {
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
                            return Ok(dml::FieldType::Relation(dml::RelationInfo::new(&type_name, "")))
                        }
                        ast::ModelOrEnum::Enum(en) if en.name == *type_name => {
                            return Ok(dml::FieldType::Enum(String::from(type_name)))
                        }
                        _ => {}
                    }
                }

                Err(vec![DirectiveValidationError::new(
                    "Unknown type encountered.",
                    "",
                    span,
                )])
            }
        }
    }
}
