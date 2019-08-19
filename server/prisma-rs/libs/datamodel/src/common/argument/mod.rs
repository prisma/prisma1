use crate::ast;
use crate::common::value;
use crate::errors::{ErrorCollection, ValidationError};
use std::collections::HashSet;

/// Represents a list of arguments.
///
/// This class makes it more convenient to implement
/// custom directives.
pub struct Arguments<'a> {
    arguments: &'a [ast::Argument],
    used_arguments: HashSet<&'a str>,
    span: ast::Span,
}

impl<'a> Arguments<'a> {
    /// Creates a new instance, given a list of arguments.
    pub fn new(arguments: &'a [ast::Argument], span: ast::Span) -> Arguments<'a> {
        Arguments {
            used_arguments: HashSet::new(),
            arguments,
            span,
        }
    }

    /// Creates empty arguments. The vec is a dummy that needs to be handed in
    /// due of the shape of the struct.
    pub fn empty(vec: &'a [ast::Argument]) -> Self {
        Arguments {
            used_arguments: HashSet::new(),
            arguments: vec,
            span: ast::Span::empty(),
        }
    }

    /// Checks if arguments occur twice and returns an appropriate error list.
    pub fn check_for_duplicate_arguments(&self) -> Result<(), ErrorCollection> {
        let mut arg_names: HashSet<&'a str> = HashSet::new();
        let mut errors = ErrorCollection::new();

        for arg in self.arguments {
            if arg_names.contains::<&str>(&(&arg.name.name as &str)) {
                errors.push(ValidationError::new_duplicate_argument_error(&arg.name.name, arg.span));
            }
            arg_names.insert(&arg.name.name);
        }

        errors.ok()
    }

    /// Checks if arguments were not accessed and raises the appropriate errors.
    pub fn check_for_unused_arguments(&self) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();

        for arg in self.arguments {
            if !self.used_arguments.contains::<&str>(&(&arg.name.name as &str)) {
                errors.push(ValidationError::new_unused_argument_error(&arg.name.name, arg.span));
            }
        }

        errors.ok()
    }

    /// Gets the span of all arguments wrapped by this instance.
    pub fn span(&self) -> ast::Span {
        self.span
    }

    /// Gets the arg with the given name.
    pub fn arg(&mut self, name: &str) -> Result<value::ValueValidator, ValidationError> {
        match self.arg_internal(name) {
            None => Err(ValidationError::new_argument_not_found_error(name, self.span)),
            Some(arg) => value::ValueValidator::new(&arg.value),
        }
    }

    /// Gets the full argument span for an argument, used to generate errors.
    fn arg_internal(&mut self, name: &str) -> Option<&'a ast::Argument> {
        for arg in self.arguments {
            if arg.name.name == name {
                self.used_arguments.insert(&arg.name.name as &str);
                return Some(&arg);
            }
        }

        None
    }

    /// Gets the arg with the given name, or if it is not found, the first unnamed argument.
    ///
    /// Use this to implement unnamed argument behavior.
    pub fn default_arg(&mut self, name: &str) -> Result<value::ValueValidator, ValidationError> {
        match (self.arg_internal(name), self.arg_internal("")) {
            (Some(arg), None) => value::ValueValidator::new(&arg.value),
            (None, Some(arg)) => value::ValueValidator::new(&arg.value),
            (Some(arg), Some(_)) => Err(ValidationError::new_duplicate_default_argument_error(&name, arg.span)),
            (None, None) => Err(ValidationError::new_argument_not_found_error(name, self.span)),
        }
    }
}
