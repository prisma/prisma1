use lazy_static::lazy_static;
use rust_inflector::Inflector as RustInflector;
use std::collections::HashMap;

// This is a remnant from the Scala inflector
lazy_static! {
    pub static ref SINGULARIZE_EXCEPTIONS: HashMap<&'static str, &'static str> =
        vec![("todoes", "todo"), ("children", "child"), ("campuses", "campus")].into_iter().collect();
    pub static ref PLURALIZE_EXCEPTIONS: HashMap<&'static str, &'static str> =
        vec![("todo", "todoes"), ("child", "children"), ("campus", "campuses")].into_iter().collect();
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
