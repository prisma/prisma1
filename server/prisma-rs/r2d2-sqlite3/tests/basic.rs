use tempdir;

use std::sync::mpsc;
use std::thread;

use r2d2::ManageConnection;
use tempdir::TempDir;

use r2d2_sqlite3::SqliteConnectionManager;
use sqlite::Connection;


#[test]
fn basic() {
    assert!(true)
}