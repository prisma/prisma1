#![deny(warnings)]

#[macro_use]
extern crate serde_derive;

pub mod config;
pub mod error;

pub type PrismaResult<T> = Result<T, error::Error>;
