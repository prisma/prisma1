//! Sqlite3 support for the `r2d2` connection pool.
//! 
//! 
// #![deny(warnings)] 

use r2d2;
use sqlite;

use std::path::{Path, PathBuf};
use std::fmt::{self, Debug};

/// An `r2d2::ManageConnection` for `sqlite::Connection`s.
pub struct SqliteConnectionManager {
    path: String,
}

impl fmt::Debug for SqliteConnectionManager {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "SqliteConnectionManager")
    }
}

impl r2d2::ManageConnection for SqliteConnectionManager {
    type Connection = sqlite::Connection;
    type Error = sqlite::Error;

    fn connect(&self) -> Result<sqlite::Connection, sqlite::Error> {
        sqlite::Connection::open(&self.path)

        // match self.source {
        //     Source::File(ref path) => Connection::open_with_flags(path, self.flags),
        //     Source::Memory => Connection::open_in_memory_with_flags(self.flags),
        // }.map_err(Into::into)
        // .and_then(|c| match self.init {
        //     None => Ok(c),
        //     Some(ref init) => init(&c).map(|_| c),
        // })
    }

    fn is_valid(&self, conn: &mut sqlite::Connection) -> Result<(), sqlite::Error> {
        conn.execute("")
    }

    fn has_broken(&self, _: &mut sqlite::Connection) -> bool {
        false
    }
}

