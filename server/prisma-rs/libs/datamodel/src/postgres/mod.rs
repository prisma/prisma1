use crate::dml;
use crate::ast;

use crate::dml::{ TypePack, Attachment, EmptyAttachment };
use crate::dml::validator::AttachmentValidator;
use crate::dml::validator::directive::builtin::{DirectiveListValidator};
use crate::dml::validator::directive::{Args, Error, DirectiveValidator};

use std::collections::HashMap;

// Attachment struct for the specialized property.
#[derive(Debug, PartialEq, Clone)]
pub struct PostgresSpecialFieldProps {
    special_prop: Option<String>
}

impl Attachment for PostgresSpecialFieldProps {
    fn default() -> Self {
        PostgresSpecialFieldProps {
            special_prop: None
        }
    }
}

// Type definitions for extending the datamodel.
#[derive(Debug, PartialEq, Clone)]
pub struct PostgresTypePack { }

impl TypePack for PostgresTypePack {
    type FieldAttachment = PostgresSpecialFieldProps;

    type EnumAttachment = EmptyAttachment;
    type ModelAttachment = EmptyAttachment;
    type SchemaAttachment = EmptyAttachment;
    type RelationAttachment = EmptyAttachment;
}

// Validator for the special directive.
pub struct PostgresSpecialPropValdiator { }
impl<Types: dml::TypePack, T: dml::WithDatabaseName> DirectiveValidator<T, Types> for PostgresSpecialPropValdiator {
    fn directive_name(&self) -> &'static str{ &"postgres.specialProp" }
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Option<Error> {

        // This is a single arg, where the name can be omitted.
        match args.default_arg("value").as_str() {
            Ok(value) => obj.set_database_name(&Some(value)),
            Err(err) => return Some(err)
        };

        return None
    }
}

// We can fix the TypePack to our specialized version here.
type Types = PostgresTypePack;

// Attachement Validator Impl.
// TODO: I think we can safe a lot of lines here if we build upon the directive mechanism. 
pub struct PostgresAttachmentValidator {
    field_directives: DirectiveListValidator<dml::Field<PostgresTypePack>, Types>
}

fn postgres_field_directives<Types: TypePack>() -> DirectiveListValidator<dml::Field<Types>, Types> {
    let mut validator = DirectiveListValidator::<dml::Field<Types>, Types>::new();
    validator.add(Box::new(PostgresSpecialPropValdiator { }));
    return validator;
}


impl AttachmentValidator<Types> for PostgresAttachmentValidator {
    fn new() -> Self {
        PostgresAttachmentValidator {
            field_directives: postgres_field_directives()
        }
    }

    fn validate_field_attachment(&self, ast_field: &ast::Field, field: &mut dml::Field<Types>) { 
        self.field_directives.validate_and_apply(ast_field, field);
    }

    // Default impl.
    fn validate_model_attachment(&self, ast_field: &ast::Model, field: &mut dml::Model<Types>) { }
    fn validate_enum_attachment(&self, ast_field: &ast::Enum, field: &mut dml::Enum<Types>) { }
    fn validate_schema_attachment(&self, ast_field: &ast::Schema, field: &mut dml::Schema<Types>) { }
    fn validate_relation_attachment(&self, ast_field: &ast::Field, field: &mut dml::RelationInfo<Types>) { }
}