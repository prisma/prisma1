use crate::{
    connector::Connector,
    error::Error,
    querying::{NodeSelector, PrismaValue},
    schema::{Field, TypeIdentifier},
    PrismaResult,
};
use arc_swap::ArcSwap;
use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{types::ToSql, Row, NO_PARAMS};
use std::{collections::HashSet, sync::Arc};

use sql::{
    prelude::*,
    grammar::operation::eq::Equable,
};


type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Databases = ArcSwap<HashSet<String>>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    databases: Databases,
}

impl Sqlite {
    pub fn new(connection_limit: u32) -> PrismaResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        let mut conn = pool.get()?;
        let databases = ArcSwap::from(Arc::new(HashSet::new()));
        let this = Sqlite { pool, databases };

        this.update_databases(&mut conn)?;

        Ok(this)
    }

    /// Updates the set of existing databases for caching purposes.
    fn update_databases(&self, conn: &mut Connection) -> PrismaResult<()> {
        let mut stmt = conn.prepare("PRAGMA database_list")?;

        let databases: HashSet<String> = stmt
            .query_map(NO_PARAMS, |row| {
                let name: String = row.get(1);
                name
            })?
            .map(|res| res.unwrap())
            .collect();

        self.databases.store(Arc::new(databases));

        Ok(())
    }

    fn has_database(&self, db_name: &str) -> bool {
        self.databases.load().contains(db_name)
    }

    fn create_database(conn: &mut Connection, db_name: &str) -> PrismaResult<()> {
        let path = format!("db/{}", db_name);
        dbg!(conn.execute("ATTACH DATABASE ? AS ?", &[path.as_ref(), db_name])?);

        Ok(())
    }

    /// Take a new connection from the pool and create the database if it
    /// doesn't exist yet.
    fn with_connection<F, T>(&self, db_name: &str, mut f: F) -> PrismaResult<T>
    where
        F: FnMut(Connection) -> PrismaResult<T>,
    {
        let mut conn = dbg!(self.pool.get()?);

        if !self.has_database(db_name) {
            // We might have a race if having more clients for the same
            // databases. Create will silently fail and we'll get the database
            // name to our bookkeeping.
            Self::create_database(&mut conn, db_name)?;
            self.update_databases(&mut conn)?;
        }

        f(conn)
    }

    fn fetch_value(typ: TypeIdentifier, row: &Row, i: usize) -> PrismaValue {
        match typ {
            TypeIdentifier::String => PrismaValue::String(row.get(i)),
            TypeIdentifier::UUID => PrismaValue::Uuid(row.get(i)),
            TypeIdentifier::Float => PrismaValue::Float(row.get(i)),
            TypeIdentifier::Int => PrismaValue::Int(row.get(i)),
            TypeIdentifier::Boolean => PrismaValue::Boolean(row.get(i)),
            TypeIdentifier::Enum => PrismaValue::Enum(row.get(i)),
            TypeIdentifier::Json => PrismaValue::Json(row.get(i)),
            TypeIdentifier::DateTime => PrismaValue::DateTime(row.get(i)),
            TypeIdentifier::GraphQLID => PrismaValue::GraphQLID(row.get(i)),
            TypeIdentifier::Relation => {
                let value: i64 = row.get(i);
                PrismaValue::Relation(value as u64)
            }
        }
    }
}

impl Connector for Sqlite {
    fn select_1(&self) -> PrismaResult<i32> {
        let conn = self.pool.get()?;
        let mut stmt = conn.prepare("SELECT 1")?;
        let mut rows = stmt.query_map(NO_PARAMS, |row| row.get(0))?;

        match rows.next() {
            Some(r) => Ok(r?),
            None => Err(Error::NoResultsError),
        }
    }

    fn get_node_by_where(
        &self,
        database_name: &str,
        selector: &NodeSelector,
    ) -> PrismaResult<Vec<PrismaValue>> {
        self.with_connection(database_name, |conn| {
            let mut result = Vec::new();
            let select_fields: &[Field] = &selector.selected_fields;
            let field_names: Vec<&str> = select_fields
                .iter()
                .map(|field| field.name.as_ref())
                .collect();

            let query = dbg!(
                select_from(&selector.table)
                    .columns(field_names.as_slice())
                    .so_that(selector.field.name.equals(DatabaseValue::Parameter))
                    .compile()
                    .unwrap()
            );

            let params = vec![(selector.value as &ToSql)];

            conn.query_row(&query, params.as_slice(), |row| {
                for (i, field) in selector.model.fields.iter().enumerate() {
                    result.push(Self::fetch_value(field.type_identifier, row, i));
                }
            })?;

            Ok(result)
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{connector::Connector, querying::*, schema::*};

    #[test]
    fn test_select_1() {
        let sqlite = Sqlite::new(1).unwrap();
        assert_eq!(1, sqlite.select_1().unwrap());
    }

    #[test]
    fn test_simple_select_by_where() {
        let sqlite = Sqlite::new(1).unwrap();
        let db_name = "graphcool";

        // Create a simple schema
        sqlite
            .with_connection(db_name, |conn| {
                conn.execute_batch(
                    "BEGIN;
                 DROP TABLE IF EXISTS graphcool.user;
                 CREATE TABLE graphcool.user(id INTEGER PRIMARY KEY, name TEXT NOT NULL);
                 INSERT INTO graphcool.user (name) values ('Musti');
                 COMMIT;",
                )
                .unwrap();
                Ok(())
            })
            .unwrap();

        let fields = vec![
            Field {
                name: String::from("id"),
                type_identifier: TypeIdentifier::Int,
                is_required: true,
                is_list: false,
                is_unique: true,
                is_hidden: false,
                is_readonly: true,
                is_auto_generated: true,
            },
            Field {
                name: String::from("name"),
                type_identifier: TypeIdentifier::String,
                is_required: true,
                is_list: false,
                is_unique: false,
                is_hidden: false,
                is_readonly: false,
                is_auto_generated: false,
            },
        ];

        let model = Model {
            name: String::from("User"),
            stable_identifier: String::from("user"),
            is_embedded: false,
            fields: fields,
        };

        let find_by = PrismaValue::String(String::from("Musti"));
        let selector =
            NodeSelector::new(db_name, &model, &model.fields[1], &find_by, &model.fields);

        let result = sqlite.get_node_by_where(db_name, &selector).unwrap();

        assert_eq!(2, result.len());
        assert_eq!(PrismaValue::Int(1), result[0]);
        assert_eq!(PrismaValue::String(String::from("Musti")), result[1]);
    }
}
