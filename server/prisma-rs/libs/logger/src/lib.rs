#![deny(warnings)]

#[macro_use]
extern crate slog;
#[macro_use]
extern crate slog_scope;

mod logger;

pub use self::logger::*;
