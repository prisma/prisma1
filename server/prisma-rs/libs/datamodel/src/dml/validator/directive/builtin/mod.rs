use crate::ast;
use crate::dml;
use crate::dml::validator::argument::DirectiveArguments;
use crate::dml::validator::directive::DirectiveValidator;
use crate::errors::{DirectiveValidationError, ErrorCollection};

use std::collections::HashMap;

mod db;
mod default;
mod embedded;
mod ondelete;
mod primary;
mod relation;
mod scalarlist;
mod sequence;
mod unique;

// TODO: This should not be in the builtin mod.
pub struct DirectiveListValidator<T> {
    known_directives: HashMap<&'static str, Box<DirectiveValidator<T>>>,
}

impl<T> DirectiveListValidator<T> {
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

        self.known_directives.insert(name, validator);
    }

    pub fn validate_and_apply(&self, ast: &ast::WithDirectives, t: &mut T) -> Result<(), ErrorCollection> {
        let mut errors = ErrorCollection::new();

        for directive in ast.directives() {
            match self.known_directives.get(directive.name.as_str()) {
                Some(validator) => {
                    if let Err(err) = validator.validate_and_apply(
                        &DirectiveArguments::new(&directive.arguments, &directive.name, directive.span),
                        t,
                    ) {
                        errors.push(Box::new(err));
                    }
                }
                None => errors.push(Box::new(DirectiveValidationError::new(
                    "Encountered unknown directive",
                    &directive.name,
                    &directive.span,
                ))),
            };
        }

        if errors.has_errors() {
            Err(errors)
        } else {
            Ok(())
        }
    }
}

pub fn new_builtin_field_directives() -> DirectiveListValidator<dml::Field> {
    let mut validator = DirectiveListValidator::<dml::Field> {
        known_directives: HashMap::new(),
    };

    validator.add(Box::new(db::DbDirectiveValidator {}));
    validator.add(Box::new(primary::PrimaryDirectiveValidator {}));
    validator.add(Box::new(scalarlist::ScalarListDirectiveValidator {}));
    validator.add(Box::new(sequence::SequenceDirectiveValidator {}));
    validator.add(Box::new(unique::UniqueDirectiveValidator {}));
    validator.add(Box::new(default::DefaultDirectiveValidator {}));
    validator.add(Box::new(relation::RelationDirectiveValidator {}));
    validator.add(Box::new(ondelete::OnDeleteDirectiveValidator {}));

    return validator;
}

pub fn new_builtin_model_directives() -> DirectiveListValidator<dml::Model> {
    let mut validator = DirectiveListValidator::<dml::Model> {
        known_directives: HashMap::new(),
    };

    validator.add(Box::new(db::DbDirectiveValidator {}));
    validator.add(Box::new(embedded::EmbeddedDirectiveValidator {}));

    return validator;
}

pub fn new_builtin_enum_directives() -> DirectiveListValidator<dml::Enum> {
    let validator = DirectiveListValidator::<dml::Enum> {
        known_directives: HashMap::new(),
    };

    // Adds are missing

    return validator;
}
