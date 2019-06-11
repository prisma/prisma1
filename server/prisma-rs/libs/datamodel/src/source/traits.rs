use crate::common::argument::Arguments;
use crate::dml;
use crate::dml::validator::directive::DirectiveValidator;
use crate::errors::ValidationError;
use std::collections::HashMap;

// TODO: Probably rename everything. Terminology here is messy.

/// Trait for custom sources.
///
/// A source is basically the datamodel equivalent of a connector.
pub trait Source {
    /// Gets the name of the implementing connector.
    fn connector_type(&self) -> &str;
    /// Gets the name of the source configuration block.
    fn name(&self) -> &String;
    /// Gets the source config URL.
    fn url(&self) -> &String;
    /// Gets all custom configuration attributes.
    // TODO: String is probably a bad choice. Prisma value would be better.
    fn config(&self) -> HashMap<String, String>;
    /// Gets all field directives defined by this source.
    ///
    /// The directives returned here are unscoped.
    fn get_field_directives(&self) -> Vec<Box<DirectiveValidator<dml::Field>>>;
    /// Gets all model directives defined by this source.
    ///
    /// The directives returned here are unscoped.
    fn get_model_directives(&self) -> Vec<Box<DirectiveValidator<dml::Model>>>;
    /// Gets all enum directives defined by this source.
    ///
    /// The directives returned here are unscoped.
    fn get_enum_directives(&self) -> Vec<Box<DirectiveValidator<dml::Enum>>>;

    /// Documentation of this source.
    fn documentation(&self) -> &Option<String>;
}

/// Trait for source definitions.
///
/// It provides access to the source's name, as well as a factory method.
pub trait SourceDefinition {
    /// Returns the name of the source.
    fn connector_type(&self) -> &'static str;
    /// Instantiates a new source, using the given name, url and detailed arguments.
    fn create(
        &self,
        name: &str,
        url: &str,
        arguments: &Arguments,
        documentation: &Option<String>,
    ) -> Result<Box<Source>, ValidationError>;
}
