#![deny(warnings)]

#[macro_use]
extern crate serde_derive;
#[macro_use]
extern crate slog;
#[macro_use]
extern crate slog_scope;

pub mod config;
pub mod error;
pub mod logger;
