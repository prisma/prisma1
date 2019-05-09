use crate::ast;
use crate::dml;
use crate::dml::validator::argument::DirectiveArguments;
use crate::dml::validator::directive::DirectiveValidator;

use std::collections::HashMap;

mod db;
mod primary;
mod embedded;
mod scalarlist;
mod sequence;
mod default;
mod unique;
mod ondelete;
mod relation;

pub struct DirectiveListValidator<T> {
    known_directives: HashMap<&'static str, Box<DirectiveValidator<T>>>,
}

impl<T> DirectiveListValidator<T> {
    pub fn add(&mut self, validator: Box<DirectiveValidator<T>>) {
        let name = validator.directive_name();

        if self.known_directives.contains_key(name) {
            panic!("Duplicate directive definition: {:?}", name);
        }

        self.known_directives.insert(name, validator);
    }

    pub fn validate_and_apply(&self, ast: &ast::WithDirectives, t: &mut T) {
        for directive in ast.directives() {
            match self.known_directives.get(directive.name.as_str()) {
                Some(validator) => validator.validate_and_apply(&DirectiveArguments::new(&directive.arguments), t),
                None => panic!("Encountered unknown directive: {:?}", directive.name),
            };
        }
    }
}

pub fn new_field_directives() -> DirectiveListValidator<dml::Field> {
    let mut validator = DirectiveListValidator::<dml::Field> { known_directives: HashMap::new() };

    validator.add(Box::new(db::DbDirectiveValidator{ }));
    validator.add(Box::new(primary::PrimaryDirectiveValidator{ }));
    validator.add(Box::new(scalarlist::ScalarListDirectiveValidator{ }));
    validator.add(Box::new(sequence::SequenceDirectiveValidator{ }));
    validator.add(Box::new(unique::UniqueDirectiveValidator{ }));
    validator.add(Box::new(default::DefaultDirectiveValidator{ }));
    validator.add(Box::new(relation::RelationDirectiveValidator{ }));
    validator.add(Box::new(ondelete::OnDeleteDirectiveValidator{ }));

    return validator;
}

pub fn new_model_directives() -> DirectiveListValidator<dml::Model> {
    let mut validator = DirectiveListValidator::<dml::Model> { known_directives: HashMap::new() };

    validator.add(Box::new(db::DbDirectiveValidator {}));
    validator.add(Box::new(embedded::EmbeddedDirectiveValidator {}));

    return validator;
}

pub fn new_enum_directives() -> DirectiveListValidator<dml::Enum> {
    let mut validator = DirectiveListValidator::<dml::Enum> {
        known_directives: HashMap::new(),
    };

    // Adds are missing

    return validator;
}
