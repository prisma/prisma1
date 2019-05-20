use crate::common::argument::Arguments;
use crate::dml;
use crate::dml::validator::directive::DirectiveValidator;
use crate::errors::ValidationError;

pub trait Source {
    fn name(&self) -> &String;
    fn url(&self) -> &String;
    fn get_field_directives(&self) -> Vec<Box<DirectiveValidator<dml::Field>>>;
    fn get_model_directives(&self) -> Vec<Box<DirectiveValidator<dml::Model>>>;
    fn get_enum_directives(&self) -> Vec<Box<DirectiveValidator<dml::Enum>>>;
}

pub trait SourceDefinition {
    fn name(&self) -> &'static str;
    fn create(&self, name: &str, url: &str, arguments: &Arguments) -> Result<Box<Source>, ValidationError>;
}
