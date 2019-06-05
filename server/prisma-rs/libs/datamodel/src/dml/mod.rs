// TODOs to answer together with rust teams:
// * Should this structure be mutatble or immutable?
// * Should this structure contain circular references? (Would make renaming models/fields MUCH easier)
// * How do we handle ocnnector specific settings, like indeces? Maybe inheritance, traits and having a Connector<T>?
mod comment;
mod datamodel;
mod enummodel;
mod field;
mod id;
mod model;
mod relation;
mod scalar;
mod traits;

pub use self::datamodel::*;
pub use comment::*;
pub use enummodel::*;
pub use field::*;
pub use id::*;
pub use model::*;
pub use relation::*;
pub use scalar::*;
pub use traits::*;

pub mod validator;

// Compatibility export.
pub use crate::common::PrismaType as ScalarType;
pub use crate::common::PrismaValue as Value;