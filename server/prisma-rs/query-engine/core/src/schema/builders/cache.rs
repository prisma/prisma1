///! Every builder is required to cache input and output type refs that are created inside
///! of them, e.g. as soon as the builder produces a ref, it must be retrievable later.
///!
///! This has two purposes:
///! - First, circular dependencies, as they can happen in input / output types, are broken.
///! - Second, it serves as a central list of build types of that builder, which are used later to
///!   collect all types of the query schema.
///!
///! The cached types are stored as Arcs, and the cache owns these (strong) Arcs,
///! while the cache will only hand out weak arcs. not only does this simplify the builder architecture,
///! but also prevents issues with memory leaks in the schema, as well as issues that when all strong
///! arcs are dropped due to visitor operations, the schema can't be traversed anymore due to invalid references
///!
use super::*;
use std::{
    cell::RefCell,
    collections::HashMap,
    sync::{Arc, Weak},
};

pub trait CachedBuilder<T> {
    /// Retrieve cache.
    fn get_cache(&self) -> &TypeRefCache<T>;

    /// Caches the given arc.
    fn cache(&self, key: String, value: Arc<T>) {
        self.get_cache().insert(key, value);
    }

    /// Consumes the implementing builder and returns all strong refs of type T that are cached on .
    fn into_strong_refs(self) -> Vec<Arc<T>>;
}

/// Cache wrapper with internal mutability
pub struct TypeRefCache<T> {
    cache: RefCell<HashMap<String, Arc<T>>>,
}

impl<T> TypeRefCache<T> {
    pub fn new() -> Self {
        TypeRefCache {
            cache: RefCell::new(HashMap::new()),
        }
    }

    pub fn get(&self, key: &str) -> Option<Weak<T>> {
        self.cache.borrow().get(key).map(|v| Arc::downgrade(v))
    }

    pub fn insert(&self, key: String, value: Arc<T>) {
        self.cache.borrow_mut().insert(key, value);
    }
}

impl<T> Into<Vec<Arc<T>>> for TypeRefCache<T> {
    fn into(self) -> Vec<Arc<T>> {
        let mut vec: Vec<Arc<T>> = vec![];

        vec.extend(self.cache.into_inner().into_iter().map(|(_, t)| t));
        vec
    }
}

impl<T> From<Vec<(String, Arc<T>)>> for TypeRefCache<T> {
    fn from(tuples: Vec<(String, Arc<T>)>) -> TypeRefCache<T> {
        TypeRefCache {
            cache: RefCell::new(tuples.into_iter().collect()),
        }
    }
}

impl<T> IntoIterator for TypeRefCache<T> {
    type Item = (String, Arc<T>);
    type IntoIter = ::std::collections::hash_map::IntoIter<String, Arc<T>>;

    fn into_iter(self) -> Self::IntoIter {
        self.cache.into_inner().into_iter()
    }
}

/// Cache utility to load and return immediately if a type is already cached.
macro_rules! return_cached {
    ($cache:expr, $name:expr) => {
        let existing_type = $cache.get($name);
        if existing_type.is_some() {
            return existing_type.unwrap();
        }
    };
}
