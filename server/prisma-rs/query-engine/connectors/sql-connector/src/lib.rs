//! # The SQL Connector interface
//!
//! The public interface to outside is split into separate traits:
//!
//! - [DataResolver](../connector/trait.DataResolver.html) to fetch data.
//! - [DatabaseMutactionExecutor](../connector/trait.DatabaseMutactionExecutor.html) to write
//!   data.

mod cursor_condition;
mod filter_conversion;
mod mutaction;
mod ordering;
mod query_builder;
mod row;
mod transactional;

pub mod database;

pub use filter_conversion::*;
pub use mutaction::*;
pub use query_builder::SelectDefinition;
pub use row::*;
pub use transactional::*;
