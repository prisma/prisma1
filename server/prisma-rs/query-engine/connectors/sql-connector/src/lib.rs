//! # The SQL Connector interface
//!
//! The public interface to outside is split into separate traits:
//!
//! - [DataResolver](../connector/trait.DataResolver.html) to fetch data.
//! - [DatabaseMutactionExecutor](../connector/trait.DatabaseMutactionExecutor.html) to write
//!   data.

mod cursor_condition;
mod database;
mod error;
mod filter_conversion;
mod mutaction;
mod ordering;
mod query_builder;
mod row;
mod transactional;

use filter_conversion::*;
use mutaction::*;
use row::*;
use transactional::*;

pub use database::*;

type SqlResult<T> = Result<T, error::SqlError>;
