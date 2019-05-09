use crate::{dml, ast};

pub mod value;
pub mod argument;
pub mod directive;

use value::{WrappedValue, ValueValidator};
use directive::builtin::{DirectiveListValidator, new_field_directives, new_model_directives, new_enum_directives};

pub struct Validator {
    pub field_directives: DirectiveListValidator<dml::Field>,
    pub model_directives: DirectiveListValidator<dml::Model>,
    pub enum_directives: DirectiveListValidator<dml::Enum>
}

impl Validator {

    pub fn new() -> Validator {
        Validator {
            field_directives: new_field_directives(),
            model_directives: new_model_directives(),
            enum_directives: new_enum_directives()
        }
    }

    pub fn validate(&self, ast_schema: &ast::Schema) -> dml::Schema {
        let mut schema = dml::Schema::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::ModelOrEnum::Enum(en) => schema.models.push(dml::ModelOrEnum::Enum(self.validate_enum(&en))),
                ast::ModelOrEnum::Model(ty) => schema.models.push(dml::ModelOrEnum::Model(self.validate_model(&ty)))
            }
        }

        // TODO: This needs some resolver logic for enum and relation types.
        return schema
    }

    fn validate_model(&self, ast_model: &ast::Model) -> dml::Model {
        let mut ty = dml::Model::new(&ast_model.name);
        self.model_directives.validate_and_apply(ast_model, &mut ty);

        for ast_field in &ast_model.fields {
            ty.fields.push(self.validate_field(ast_field));
        }

        return ty
    }

    fn validate_enum(&self, ast_enum: &ast::Enum) -> dml::Enum {
        unimplemented!("Parsing enums is not implemented yet.");
    }

    fn validate_field(&self, ast_field: &ast::Field) -> dml::Field {
        let field_type = self.validate_field_type(&ast_field.field_type);

        let mut field = dml::Field::new(&ast_field.name, &field_type);

        field.arity = self.validate_field_arity(&ast_field.arity);

        if let Some(value) = &ast_field.default_value {
            if let dml::FieldType::Base(base_type) = &field_type {
                // TODO: Proper error handling.
                // TODO: WrappedValue is not the tool of choice here,
                // there should be a static func for converting stuff.
                field.default_value = Some((WrappedValue { value: value.clone() }).as_type(base_type).expect("Unable to parse."));
            } else {
                unimplemented!("Found a default value for a non-scalar type.")
            }
        }

        self.field_directives.validate_and_apply(ast_field, &mut field);

        return field
    }

    fn validate_field_arity(&self, ast_field: &ast::FieldArity) -> dml::FieldArity {
        match ast_field {
            ast::FieldArity::Required => dml::FieldArity::Required,
            ast::FieldArity::Optional => dml::FieldArity::Optional,
            ast::FieldArity::List => dml::FieldArity::List
        }
    }

    fn validate_field_type(&self, type_name: &String) -> dml::FieldType {
        match type_name.as_ref() {
            "Int" => dml::FieldType::Base(dml::ScalarType::Int),
            "Float" => dml::FieldType::Base(dml::ScalarType::Float),
            "Decimal" => dml::FieldType::Base(dml::ScalarType::Decimal),
            "Boolean" => dml::FieldType::Base(dml::ScalarType::Boolean),
            "String" => dml::FieldType::Base(dml::ScalarType::String),
            "DateTime" => dml::FieldType::Base(dml::ScalarType::DateTime),
            // Everything is a relation for now.
            _ => dml::FieldType::Relation { to: type_name.to_string(), to_field: String::from(""), name: None, on_delete: dml::OnDeleteStrategy::None }
        }
    }
}