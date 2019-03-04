#[cfg(not(all(feature = "barrel", feature = "rusqlite")))]
compile_error!("Add `--all-features` to your cargo command to run this example");

use barrel::{backend::Sqlite as Squirrel, types, Migration};
use rusqlite::{Result, Connection, NO_PARAMS};

/// Create a few simple tables via migrations
fn generate_migration() -> String {
    let mut m = Migration::new();
    m.create_table("prisma", |t| {
        t.add_column("name", types::varchar(255));
        t.add_column("location", types::foreign("cities"));
        t.add_column("founded", types::date());
    });

    m.create_table("cities", |t| {
        t.add_column("long", types::float());
        t.add_column("name", types::varchar(255));
        t.add_column("lat", types::float());
    });

    m.make::<Squirrel>()
}

/// Create an in-memory database and run migrations on it
fn create_database(sql: String) -> Connection {
    Connection::open_in_memory()
        .and_then(|c| c.execute(&sql, NO_PARAMS).map(|_| c))
        .map_err(|e| panic!(e))
        .unwrap()
}

/// Use prisma-query to get the table names
fn query_tables(c: &mut Connection) -> Vec<String> {
    let sql = "SELECT name FROM sqlite_master WHERE type='table'";

    (|| -> Result<()> {
        let mut stmt = c.prepare(sql)?;
        let mut rows = stmt.query(NO_PARAMS)?;

        while let Some(r) = rows.next() {
            println!("ROW");
        }

        Ok(())
    })().map_err(|e| panic!(e));

    vec![]
}

fn main() {
    let mut c = create_database(generate_migration());
    println!("{:?}", query_tables(&mut c));
}
