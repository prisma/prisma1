use crate::dml;

use crate::dml::validator::directive::builtin::DirectiveListValidator;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};
use crate::dml::validator::{AttachmentDirectiveSource, AttachmentDirectiveValidator};

// Validator for the special directive.
pub struct PostgresSpecialPropValidator {}
impl DirectiveValidator<dml::Field> for PostgresSpecialPropValidator {
    fn directive_name(&self) -> &'static str {
        &"postgres.specialProp"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field) -> Option<Error> {
        // This is a single arg, where the name can be omitted.
        match args.default_arg("value").as_str() {
            Ok(value) => obj.database_name = Some(value),
            Err(err) => return self.parser_error(&err),
        };

        return None;
    }
}

// Attachement Validator Implementation. Minimal variant for directives.
// Alternatively, we could use the AttachmendValidator trait to get more control.
pub struct PostgresDirectives {}

impl AttachmentDirectiveSource for PostgresDirectives {
    fn add_field_directives(validator: &mut DirectiveListValidator<dml::Field>) {
        validator.add(Box::new(PostgresSpecialPropValidator {}));
    }
    fn add_model_directives(validator: &mut DirectiveListValidator<dml::Model>) {}
    fn add_enum_directives(validator: &mut DirectiveListValidator<dml::Enum>) {}
}

pub type PostgresAttachmentValidator = AttachmentDirectiveValidator<PostgresDirectives>;
