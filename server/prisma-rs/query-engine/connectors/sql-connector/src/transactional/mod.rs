mod database_reader;
mod database_writer;

pub(crate) mod transaction_ext;

pub use database_reader::*;
pub use database_writer::*;

use prisma_query::connector::{Transaction};

pub trait Transactional {
    fn with_transaction<F, T>(&self, db: &str, f: F) -> crate::Result<T>
    where
        F: FnOnce(&mut Transaction) -> crate::Result<T>;
}
