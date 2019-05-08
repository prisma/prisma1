use crate::dml;
use crate::dml::validator::directive::{Args, DirectiveValidator, Error};

pub struct DbDirectiveValidator {}

impl<T: dml::WithDatabaseName> DirectiveValidator<T> for DbDirectiveValidator {
    fn directive_name(&self) -> &'static str {
        &"db"
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Option<Error> {
        match args.arg("name").as_str() {
            Ok(value) => obj.set_database_name(&Some(value)),
            Err(err) => return Some(err),
        };

        return None;
    }
}
