// TODOs to answer together with rust teams:
// * Should this structure be mutatble or immutable?
// * Should this structure contain circular references? (Would make renaming models/fields MUCH easier)
// * How do we handle ocnnector specific settings, like indeces? Maybe inheritance, traits and having a Connector<T>?
mod relation;
mod attachment;
mod traits;
mod comment;
mod scalar;
mod field;
mod id;
mod enummodel;
mod model;
mod schema;

pub use relation::*;
pub use attachment::*;
pub use traits::*;
pub use comment::*;
pub use scalar::*;
pub use field::*;
pub use id::*;
pub use enummodel::*;
pub use model::*;
pub use schema::*;

pub mod validator;