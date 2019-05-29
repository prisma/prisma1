use super::*;

#[macro_use]
mod cache;
mod argument_builder;
mod filter_arguments;
mod filter_type_builder;
mod input_type_builder;
mod object_type_builder;
mod query_schema_builder;

use cache::*;
use filter_arguments::*;

pub use argument_builder::*;
pub use filter_type_builder::*;
pub use input_type_builder::*;
pub use object_type_builder::*;
pub use query_schema_builder::*;

use std::sync::{Arc, Weak};

/// Since we have the invariant that the weak refs that are used throughout the schema are always valid,
/// we use this simple trait to keep the code clutter low.
pub trait IntoArc<T> {
    fn into_arc(&self) -> Arc<T>;
}

impl<T> IntoArc<T> for Weak<T> {
    fn into_arc(&self) -> Arc<T> {
        self.upgrade().expect("Expected weak reference to be valid.")
    }
}
