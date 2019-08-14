#[rustfmt::skip]

///! Prisma connector interfaces module.
///!
///! Managed connector interfaces are for connectors that do not want full control of query AST
///! execution. Hence, they expose an interface that is a collection of fine-grained read or
///! write operations, which is used by the general (managed) query executor in the core.
///!
///! Unmanaged connector interfaces take full responsibility of query execution.

mod unmanaged_database_writer;
mod managed_database_reader;

pub use managed_database_reader::*;
pub use unmanaged_database_writer::*;
