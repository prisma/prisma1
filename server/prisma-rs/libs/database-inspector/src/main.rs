use barrel::{backend::Sqlite as Squirrel, types, Migration};
use prisma_models::prelude::*;
use rusqlite::{types::ToSql, Connection, Result, NO_PARAMS};

/// Create an in-memory database and run migrations on it
fn create_database() -> Connection {
    Connection::open_in_memory()
        .and_then(|c| {
            c.execute(
                &{
                    let mut m = Migration::new();
                    m.create_table("prisma", |t| {
                        t.add_column("name", types::varchar(255));
                        t.add_column("location", types::foreign("cities"));
                        t.add_column("founded", types::date());
                    });

                    m.make::<Squirrel>()
                },
                NO_PARAMS,
            )
            .map(|_| c)
        })
        .and_then(|c| {
            c.execute(
                &{
                    let mut m = Migration::new();
                    m.create_table("cities", |t| {
                        t.add_column("long", types::float());
                        t.add_column("name", types::varchar(255));
                        t.add_column("lat", types::float());
                    });

                    m.make::<Squirrel>()
                },
                NO_PARAMS,
            )
            .map(|_| c)
        })
        .map_err(|e| panic!(e))
        .unwrap()
}

/// Use prisma-query to get the table names
fn query_tables(c: &mut Connection) -> Vec<String> {
    let sql = "SELECT name FROM sqlite_master WHERE type = 'table'";

    (|| -> Result<()> {
        let mut stmt = c.prepare(sql)?;

        let name_iter = stmt.query_map(NO_PARAMS, |row| NameCol { name: row.get(0) })?;

        name_iter
            .into_iter()
            .filter_map(|i| i.ok())
            .for_each(|i| println!("Table: {}", i.name));

        Ok(())
    })()
    .map_err(|e| panic!(e));

    vec![]
}

use time::Timespec;

#[derive(Debug)]
struct NameCol {
    name: String,
}

// struct Person {
//     id: i32,
//     name: String,
//     time_created: Timespec,
//     data: Option<Vec<u8>>,
// }

fn main() {
    let mut c = create_database();
    println!("{:?}", query_tables(&mut c));

    // c.execute(
    //     "CREATE TABLE person (
    //               id              INTEGER PRIMARY KEY,
    //               name            TEXT NOT NULL,
    //               time_created    TEXT NOT NULL,
    //               data            BLOB
    //               )",
    //     NO_PARAMS,
    // )
    // .unwrap();
    // let me = Person {
    //     id: 0,
    //     name: "Steven".to_string(),
    //     time_created: time::get_time(),
    //     data: None,
    // };
    // c.execute(
    //     "INSERT INTO person (name, time_created, data)
    //               VALUES (?1, ?2, ?3)",
    //     &[&me.name as &ToSql, &me.time_created, &me.data],
    // )
    // .unwrap();

    // let mut stmt = c
    //     .prepare("SELECT id, name, time_created, data FROM person")
    //     .unwrap();
    // let person_iter = stmt
    //     .query_map(NO_PARAMS, |row| Person {
    //         id: row.get(0),
    //         name: row.get(1),
    //         time_created: row.get(2),
    //         data: row.get(3),
    //     })
    //     .unwrap();

    // for person in person_iter {
    //     println!("Found person {:?}", person.unwrap());
    // }
}
