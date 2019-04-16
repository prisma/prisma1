use inflector::Inflector as RustInflector;
use lazy_static::lazy_static;
use std::collections::HashMap;

/// This is a remnant from the Scala inflector
lazy_static! {
    pub static ref SINGULARIZE_EXCEPTIONS: HashMap<&'static str, &'static str> =
        vec![("todoes", "todo")].into_iter().collect();
    pub static ref PLURALIZE_EXCEPTIONS: HashMap<&'static str, &'static str> =
        vec![("todo", "todoes")].into_iter().collect();
}

pub struct Inflector;

impl Inflector {
    pub fn singularize(word: &str) -> String {
        match SINGULARIZE_EXCEPTIONS.get(word).cloned() {
            Some(exception) => exception.to_owned(),
            None => word.to_camel_case().to_singular(),
        }
    }

    // TODO: Figure out why and how to use this
    #[allow(dead_code)]
    pub fn pluralize(word: &str) -> String {
        match PLURALIZE_EXCEPTIONS.get(word).cloned() {
            Some(exception) => exception.to_owned(),
            None => word.to_camel_case().to_plural(),
        }
    }
}
