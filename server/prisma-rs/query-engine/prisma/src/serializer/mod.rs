//! A modular query response serializer
//!
//! It parses PrismaQueraResults into an intermediate representation.
//! This is then used to feed different encoders (json, ...)

pub mod json;
