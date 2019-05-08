#[macro_use]
extern crate lazy_static;

mod categories;
mod exceptions;
mod inflector;
mod rules;

use inflector::{Inflector, Mode};

lazy_static! {
    pub static ref default: Inflector = Inflector::new(Mode::Anglicized);
    pub static ref classical: Inflector = Inflector::new(Mode::Classical);
}

trait Pluralize {
    fn pluralize(s: String) -> String;
}
