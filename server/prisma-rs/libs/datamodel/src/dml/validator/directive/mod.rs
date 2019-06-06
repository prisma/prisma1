use crate::ast;
use crate::common;
use crate::common::argument::Arguments;
use crate::dml;
use crate::errors::{ErrorCollection, ValidationError};

// BTreeMap has a strictly defined order.
// That's important since rendering depends on that order.
use std::collections::BTreeMap;

pub mod core;

/// The error type for directive validators.
pub type Error = ValidationError;
/// The argument type for directive validators.
pub type Args<'a> = common::argument::Arguments<'a>;

/// Trait for a directive validator.
///
/// Structs implementing this trait can be used to validate any
/// directive and to apply the directive's effect on the corresponding
/// datamodel object.
pub trait DirectiveValidator<T> {
    /// Gets the directive name.
    fn directive_name(&self) -> &str;

    /// Validates a directive and applies the directive
    /// to the given object.
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Result<(), Error>;

    /// Serilizes the given directive's arguments for rendering.
    fn serialize(&self, obj: &T, datamodel: &dml::Datamodel) -> Result<Option<ast::Directive>, Error>;

    /// Shorthand to construct an directive validation error.
    fn error(&self, msg: &str, span: &ast::Span) -> Result<(), Error> {
        Err(ValidationError::new_directive_validation_error(
            msg,
            self.directive_name(),
            span,
        ))
    }

    /// Shorthand to lift a generic parser error to an directive validation error.
    fn parser_error(&self, err: &ValidationError) -> Result<(), Error> {
        Err(ValidationError::new_directive_validation_error(
            &format!("{}", err),
            self.directive_name(),
            &err.span(),
        ))
    }
}

/// Moves an directive into a namespace scope.
///
/// This is mainly used with custom source blocks. It wraps a directive and
/// preprends the source name in front of the directive name.
pub struct DirectiveScope<T> {
    inner: Box<DirectiveValidator<T>>,
    #[allow(dead_code)]
    scope: String,
    name: String,
}

impl<T> DirectiveScope<T> {
    /// Creates a new instance, using the given directive and
    /// a namespae name.
    fn new(inner: Box<DirectiveValidator<T>>, scope: &str) -> DirectiveScope<T> {
        DirectiveScope {
            name: format!("{}.{}", scope, inner.directive_name()),
            inner,
            scope: String::from(scope),
        }
    }
}

impl<T> DirectiveValidator<T> for DirectiveScope<T> {
    fn directive_name(&self) -> &str {
        &self.name
    }
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Result<(), Error> {
        self.inner.validate_and_apply(args, obj)
    }
    fn serialize(&self, obj: &T, datamodel: &dml::Datamodel) -> Result<Option<ast::Directive>, Error> {
        self.inner.serialize(obj, datamodel)
    }
}

/// Struct which holds a list of directive validators and automatically
/// picks the right one for each directive in the given object.
pub struct DirectiveListValidator<T> {
    known_directives: BTreeMap<String, Box<DirectiveValidator<T>>>,
}

impl<T: 'static> DirectiveListValidator<T> {
    /// Creates a new instance.
    pub fn new() -> Self {
        DirectiveListValidator {
            known_directives: BTreeMap::new(),
        }
    }

    /// Adds a directive validator.
    pub fn add(&mut self, validator: Box<DirectiveValidator<T>>) {
        let name = validator.directive_name();

        if self.known_directives.contains_key(name) {
            panic!("Duplicate directive definition: {:?}", name);
        }

        self.known_directives.insert(String::from(name), validator);
    }

    /// Adds a directive validator with a namespace scope.
    pub fn add_scoped(&mut self, validator: Box<DirectiveValidator<T>>, scope: &str) {
        let boxed: Box<DirectiveValidator<T>> = Box::new(DirectiveScope::new(validator, scope));
        self.add(boxed)
    }

    /// Adds all directive validators from the given list.
    pub fn add_all(&mut self, validators: Vec<Box<DirectiveValidator<T>>>) {
        for validator in validators {
            self.add(validator);
        }
    }

    /// Adds all directive validators from the given list, with a namespace scope.
    pub fn add_all_scoped(&mut self, validators: Vec<Box<DirectiveValidator<T>>>, scope: &str) {
        for validator in validators {
            self.add_scoped(validator, scope);
        }
    }

    /// For each directive in the given object, picks the correct
    /// directive definition and uses it to validate and apply the directive.
    pub fn validate_and_apply(&self, ast: &ast::WithDirectives, t: &mut T) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();


        for directive in ast.directives() {
            for other_directive in ast.directives() {
                if directive as *const ast::Directive != other_directive as *const ast::Directive {
                    if directive.name == other_directive.name {
                        errors.push(ValidationError::new_duplicate_directive_error(&directive.name, &directive.span));
                    }
                }
            }
        }

        errors.ok()?;

        for directive in ast.directives() {
            match self.known_directives.get(directive.name.as_str()) {
                Some(validator) => {
                    let directive_validation_result =
                        validator.validate_and_apply(&Arguments::new(&directive.arguments, directive.span), t);
                    match directive_validation_result {
                        Err(ValidationError::ArgumentNotFound { argument_name, span }) => {
                            errors.push(ValidationError::new_directive_argument_not_found_error(
                                &argument_name,
                                &directive.name,
                                &span,
                            ))
                        }
                        Err(err) => {
                            errors.push(err);
                        }
                        _ => {}
                    }
                }
                None => errors.push(ValidationError::new_directive_not_known_error(
                    &directive.name,
                    &directive.span,
                )),
            };
        }

        errors.ok()?;

        Ok(())
    }

    pub fn serialize(&self, t: &T, datamodel: &dml::Datamodel) -> Result<Vec<ast::Directive>, ErrorCollection> {
        let mut errors = ErrorCollection::new();
        let mut directives: Vec<ast::Directive> = Vec::new();

        for directive in self.known_directives.values() {
            match directive.serialize(t, datamodel) {
                Ok(Some(directive)) => directives.push(directive),
                Ok(None) => {}
                Err(err) => errors.push(err),
            };
        }

        errors.ok()?;
    
        Ok(directives)
    }
}
