use crate::ast;
use crate::dml;

use crate::dml::validator::directive::builtin::DirectiveListValidator;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::dml::validator::{AttachmentDirectiveSource, AttachmentDirectiveValidator};
use crate::dml::{Attachment, EmptyAttachment, TypePack};

use std::collections::HashMap;

// Attachment struct for the specialized property.
#[derive(Debug, PartialEq, Clone)]
pub struct PostgresSpecialFieldProps {
    special_prop: Option<String>,
}

impl Attachment for PostgresSpecialFieldProps {
    fn default() -> Self {
        PostgresSpecialFieldProps { special_prop: None }
    }
}

// Type definitions for extending the datamodel.
#[derive(Debug, PartialEq, Clone)]
pub struct PostgresTypePack {}

impl TypePack for PostgresTypePack {
    type FieldAttachment = PostgresSpecialFieldProps;

    type EnumAttachment = EmptyAttachment;
    type ModelAttachment = EmptyAttachment;
    type SchemaAttachment = EmptyAttachment;
    type RelationAttachment = EmptyAttachment;
}

// Validator for the special directive.
pub struct PostgresSpecialPropValidator {}
impl<Types: dml::TypePack, T: dml::WithDatabaseName> DirectiveValidator<T, Types> for PostgresSpecialPropValidator {
    fn directive_name(&self) -> &'static str {
        &"postgres.specialProp"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Option<Error> {
        // This is a single arg, where the name can be omitted.
        match args.default_arg("value").as_str() {
            Ok(value) => obj.set_database_name(&Some(value)),
            Err(err) => return Some(err),
        };

        return None;
    }
}

// Attachement Validator Implementation. Minimal variant for directives.
// Alternatively, we could use the AttachmendValidator trait to get more control.
pub struct PostgresDirectives {}

impl AttachmentDirectiveSource<PostgresTypePack> for PostgresDirectives {
    fn add_field_directives(validator: &mut DirectiveListValidator<dml::Field<PostgresTypePack>, PostgresTypePack>) {
        validator.add(Box::new(PostgresSpecialPropValidator {}));
    }
    fn add_model_directives(validator: &mut DirectiveListValidator<dml::Model<PostgresTypePack>, PostgresTypePack>) {}
    fn add_enum_directives(validator: &mut DirectiveListValidator<dml::Enum<PostgresTypePack>, PostgresTypePack>) {}
}

pub type PostgresAttachmentValidator = AttachmentDirectiveValidator<PostgresTypePack, PostgresDirectives>;
