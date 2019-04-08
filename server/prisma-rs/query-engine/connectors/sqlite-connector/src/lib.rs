//! # The SQL Connector interface
//!
//! The public interface to outside is split into separate traits:
//!
//! - [DataResolver](../connector/trait.DataResolver.html) to fetch data.
//! - [DatabaseMutactionExecutor](../connector/trait.DatabaseMutactionExecutor.html) to write
//!   data.
//!
//! Additionally the connector splits internal logic into separate traits:
//!
//! - [DatabaseRead](trait.DatabaseRead.html) for reading from the database.
//! - [DatabaseWrite](trait.DatabaseWrite.html) for writing to the database.
//!
//! The implementation is currently only for [SQLite](struct.Sqlite.html).
mod cursor_condition;
mod database_executor;
mod database_readwrite;
mod filter_conversion;
mod mutaction;
mod ordering;
mod query_builder;
mod sqlite;

pub use database_executor::*;
pub use database_readwrite::*;
pub use filter_conversion::*;
pub use mutaction::*;
pub use query_builder::SelectDefinition;
pub use sqlite::*;
