#[macro_use]
extern crate lazy_static;

mod categories;
mod exceptions;
mod inflector;
mod rules;

use inflector::{Inflector, Mode};

lazy_static! {
    static ref DEFAULT: Inflector = Inflector::new(Mode::Anglicized);
    static ref CLASSICAL: Inflector = Inflector::new(Mode::Classical);
}

/// Default inflector, anglecized mode.
pub fn default() -> &'static Inflector {
    &DEFAULT
}

/// Inflector, classical mode.
pub fn classical() -> &'static Inflector {
    &CLASSICAL
}
