use crate::ast;
use crate::common;
use crate::common::argument::Arguments;
use crate::errors::{ErrorCollection, ValidationError};

use std::collections::HashMap;

pub mod builtin;

pub type Error = ValidationError;
pub type Args<'a> = common::argument::Arguments<'a>;

pub trait DirectiveValidator<T> {
    fn directive_name(&self) -> &str;
    fn validate_and_apply(&self, args: &Args, obj: &mut T) -> Result<(), Error>;

    fn error(&self, msg: &str, span: &ast::Span) -> Result<(), Error> {
        Err(ValidationError::new_directive_validation_error(
            msg,
            self.directive_name(),
            span,
        ))
    }

    fn parser_error(&self, err: &ValidationError) -> Result<(), Error> {
        Err(ValidationError::new_directive_validation_error(
            &format!("{}", err),
            self.directive_name(),
            &err.span(),
        ))
    }
}

pub struct DirectiveScope<T> {
    inner: Box<DirectiveValidator<T>>,
    #[allow(dead_code)]
    scope: String,
    name: String,
}

impl<T> DirectiveScope<T> {
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
}

pub struct DirectiveListValidator<T> {
    known_directives: HashMap<String, Box<DirectiveValidator<T>>>,
}

impl<T: 'static> DirectiveListValidator<T> {
    pub fn new() -> Self {
        DirectiveListValidator {
            known_directives: HashMap::new(),
        }
    }

    pub fn add(&mut self, validator: Box<DirectiveValidator<T>>) {
        let name = validator.directive_name();

        if self.known_directives.contains_key(name) {
            panic!("Duplicate directive definition: {:?}", name);
        }

        self.known_directives.insert(String::from(name), validator);
    }

    pub fn add_scoped(&mut self, validator: Box<DirectiveValidator<T>>, scope: &str) {
        let boxed: Box<DirectiveValidator<T>> = Box::new(DirectiveScope::new(validator, scope));
        self.add(boxed)
    }

    pub fn add_all(&mut self, validators: Vec<Box<DirectiveValidator<T>>>) {
        for validator in validators {
            self.add(validator);
        }
    }

    pub fn add_all_scoped(&mut self, validators: Vec<Box<DirectiveValidator<T>>>, scope: &str) {
        for validator in validators {
            self.add_scoped(validator, scope);
        }
    }

    pub fn validate_and_apply(&self, ast: &ast::WithDirectives, t: &mut T) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();

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

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(())
        }
    }
}
