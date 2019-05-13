use crate::{dml, ast};

pub mod value;
pub mod argument;
pub mod directive;

use value::{WrappedValue, ValueValidator};
use directive::builtin::{DirectiveListValidator, new_field_directives, new_model_directives, new_enum_directives};

// TODO: Naming
pub trait Validator<Types: dml::TypePack> {
    fn new() -> Self;
    fn validate(&self, ast_schema: &ast::Schema) -> dml::Schema<Types>;
}

pub trait AttachmentValidator<Types: dml::TypePack> {
    fn new() -> Self;
    fn validate_field_attachment(&self, ast_field: &ast::Field, field: &mut dml::Field<Types>);
    fn validate_model_attachment(&self, ast_field: &ast::Model, field: &mut dml::Model<Types>);
    fn validate_enum_attachment(&self, ast_field: &ast::Enum, field: &mut dml::Enum<Types>);
    fn validate_schema_attachment(&self, ast_field: &ast::Schema, field: &mut dml::Schema<Types>);
    fn validate_relation_attachment(&self, ast_field: &ast::Field, field: &mut dml::RelationInfo<Types>);
}

pub struct EmptyAttachmentValidator { }

impl<Types: dml::TypePack> AttachmentValidator<Types> for EmptyAttachmentValidator {
    fn new() -> Self { EmptyAttachmentValidator { } }
    fn validate_field_attachment(&self, ast_field: &ast::Field, field: &mut dml::Field<Types>) { }
    fn validate_model_attachment(&self, ast_field: &ast::Model, field: &mut dml::Model<Types>) { }
    fn validate_enum_attachment(&self, ast_field: &ast::Enum, field: &mut dml::Enum<Types>) { }
    fn validate_schema_attachment(&self, ast_field: &ast::Schema, field: &mut dml::Schema<Types>) { }
    fn validate_relation_attachment(&self, ast_field: &ast::Field, field: &mut dml::RelationInfo<Types>) { }
}

pub trait AttachmentDirectiveSource<Types: dml::TypePack> {
    fn add_field_directives(validator: &mut DirectiveListValidator<dml::Field<Types>, Types>);
    fn add_model_directives(validator: &mut DirectiveListValidator<dml::Model<Types>, Types>);
    fn add_enum_directives(validator: &mut DirectiveListValidator<dml::Enum<Types>, Types>);
}

// TODO: Proably we can make this just "directive source and use it everywhere.
pub struct AttachmentDirectiveValidator<Types: dml::TypePack, Attachments: AttachmentDirectiveSource<Types>> { 
    pub field_directives: DirectiveListValidator<dml::Field<Types>, Types>,
    pub model_directives: DirectiveListValidator<dml::Model<Types>, Types>,
    pub enum_directives: DirectiveListValidator<dml::Enum<Types>, Types>,
    placeholder: std::marker::PhantomData<Attachments>
}

impl<Types: dml::TypePack, Attachments: AttachmentDirectiveSource<Types>> AttachmentValidator<Types> for AttachmentDirectiveValidator<Types, Attachments> {
    fn new() -> Self { 
       let mut fields = DirectiveListValidator::<dml::Field<Types>, Types>::new();   
       let mut models = DirectiveListValidator::<dml::Model<Types>, Types>::new();   
       let mut enums = DirectiveListValidator::<dml::Enum<Types>, Types>::new();  

        Attachments::add_field_directives(&mut fields);
        Attachments::add_model_directives(&mut models);
        Attachments::add_enum_directives(&mut enums);

       AttachmentDirectiveValidator {
           field_directives: fields,
           model_directives: models,
           enum_directives: enums,
           placeholder: std::marker::PhantomData
       } 
    }
    fn validate_field_attachment(&self, ast_field: &ast::Field, field: &mut dml::Field<Types>) { 
        self.field_directives.validate_and_apply(ast_field, field);
    }
    fn validate_model_attachment(&self, ast_model: &ast::Model, model: &mut dml::Model<Types>) { 
        self.model_directives.validate_and_apply(ast_model, model);
    }
    fn validate_enum_attachment(&self, ast_enum: &ast::Enum, en: &mut dml::Enum<Types>) { 
        self.enum_directives.validate_and_apply(ast_enum, en);
    }
    fn validate_schema_attachment(&self, ast_field: &ast::Schema, field: &mut dml::Schema<Types>) { }
    fn validate_relation_attachment(&self, ast_field: &ast::Field, field: &mut dml::RelationInfo<Types>) { }
}

// TODO: Naming
pub struct BaseValidator<Types: dml::TypePack, AV: AttachmentValidator<Types>> {
    field_directives: DirectiveListValidator<dml::Field<Types>, Types>,
    model_directives: DirectiveListValidator<dml::Model<Types>, Types>,
    enum_directives: DirectiveListValidator<dml::Enum<Types>, Types>,
    attachment_validator: AV,
}

impl<Types: dml::TypePack, AV: AttachmentValidator<Types>> Validator<Types> for BaseValidator<Types, AV> {
    fn new() -> Self {
        BaseValidator {
            field_directives: new_field_directives(),
            model_directives: new_model_directives(),
            enum_directives: new_enum_directives(),
            attachment_validator: AV::new()
        }
    }


    // TODO: Intro factory methods for creating DML nodes.
    fn validate(&self, ast_schema: &ast::Schema) -> dml::Schema<Types> {
        let mut schema = dml::Schema::new();

        for ast_obj in &ast_schema.models {
            match ast_obj {
                ast::ModelOrEnum::Enum(en) => schema.add_enum(self.validate_enum(&en)),
                ast::ModelOrEnum::Model(ty) => schema.add_model(self.validate_model(&ty, ast_schema))
            }
        }

        self.attachment_validator.validate_schema_attachment(ast_schema, &mut schema);

        // TODO: This needs some resolver logic for enum and relation types.
        return schema
    }
}

impl<Types: dml::TypePack, AV: AttachmentValidator<Types>> BaseValidator<Types, AV> {
    fn validate_model(&self, ast_model: &ast::Model, ast_schema: &ast::Schema) -> dml::Model<Types> {
        let mut model = dml::Model::new(&ast_model.name);

        for ast_field in &ast_model.fields {
            model.add_field(self.validate_field(ast_field, ast_schema));
        }

        self.model_directives.validate_and_apply(ast_model, &mut model);
        self.attachment_validator.validate_model_attachment(ast_model, &mut model);

        return model
    }

    fn validate_enum(&self, ast_enum: &ast::Enum) -> dml::Enum<Types> {
        let mut en = dml::Enum::new(ast_enum.name.clone(), ast_enum.values.clone());

        self.enum_directives.validate_and_apply(ast_enum, &mut en);

        return en
    }

    fn validate_field(&self, ast_field: &ast::Field, ast_schema: &ast::Schema) -> dml::Field<Types> {
        let field_type = self.validate_field_type(&ast_field.field_type, ast_schema);

        let mut field = dml::Field::new(ast_field.name.clone(), field_type.clone());

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
        self.attachment_validator.validate_field_attachment(ast_field, &mut field);

        return field
    }

    fn validate_field_arity(&self, ast_field: &ast::FieldArity) -> dml::FieldArity {
        match ast_field {
            ast::FieldArity::Required => dml::FieldArity::Required,
            ast::FieldArity::Optional => dml::FieldArity::Optional,
            ast::FieldArity::List => dml::FieldArity::List
        }
    }

    fn validate_field_type(&self, type_name: &String, ast_schema: &ast::Schema) -> dml::FieldType<Types> {
        match type_name.as_ref() {
            "ID" => dml::FieldType::Base(dml::ScalarType::Int),
            "Int" => dml::FieldType::Base(dml::ScalarType::Int),
            "Float" => dml::FieldType::Base(dml::ScalarType::Float),
            "Decimal" => dml::FieldType::Base(dml::ScalarType::Decimal),
            "Boolean" => dml::FieldType::Base(dml::ScalarType::Boolean),
            "String" => dml::FieldType::Base(dml::ScalarType::String),
            "DateTime" => dml::FieldType::Base(dml::ScalarType::DateTime),
            // Distinguish between relation and enum.
            _ => {
                for model in &ast_schema.models {
                    match &model {
                        // TODO: Get primary field
                        ast::ModelOrEnum::Model(model) if model.name == *type_name => {
                            return dml::FieldType::Relation(dml::RelationInfo::new(type_name.clone(), String::from("")))
                        }
                        ast::ModelOrEnum::Enum(en) if en.name == *type_name => {
                            return dml::FieldType::Enum(type_name.clone())
                        }
                        _ => {}
                    }
                }

                panic!("Cannot resolve relation, type, model or enum not found: {}", type_name)
            }
        }
    }
}