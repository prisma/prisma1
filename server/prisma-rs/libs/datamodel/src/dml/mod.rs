// TODOs to answer together with rust teams:
// * Should this structure be mutatble or immutable?
// * Should this structure contain circular references? (Would make renaming models/fields MUCH easier)
// * How do we handle ocnnector specific settings, like indeces? Maybe inheritance, traits and having a Connector<T>?
mod comment;
mod enummodel;
mod field;
mod fromstr;
mod id;
mod model;
mod relation;
mod scalar;
mod datamodel;
mod traits;

pub use comment::*;
pub use enummodel::*;
pub use field::*;
pub use fromstr::*;
pub use id::*;
pub use model::*;
pub use relation::*;
pub use scalar::*;
pub use self::datamodel::*;
pub use traits::*;

pub mod validator;
