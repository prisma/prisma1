use arc_swap::ArcSwap;
use r2d2;
use r2d2_sqlite::SqliteConnectionManager;
use std::{collections::HashSet, sync::Arc};

use crate::{
    connector::Connector,
    querying::NodeSelector,
    schema::{Field, TypeIdentifier},
    PrismaResult, PrismaValue,
    error::Error,
    protobuf::prisma::{
        GraphqlId,
        graphql_id::IdValue,
    },
    SERVER_ROOT,
};

use rusqlite::{
    types::{Null, ToSql, ToSqlOutput, FromSql, ValueRef, FromSqlResult, },
    Error as RusqlError,
    Row,
    NO_PARAMS
};

use sql::{grammar::operation::eq::Equable, prelude::*};

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Databases = ArcSwap<HashSet<String>>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    databases: Databases,
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory. By querying from
    /// different databases, it will try to create them to
    /// `$SERVER_ROOT/database` if they do not exists yet.
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
        let path = format!("{}/db/{}", *SERVER_ROOT, db_name);
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
            TypeIdentifier::String    => PrismaValue::String(row.get(i)),
            TypeIdentifier::GraphQLID => PrismaValue::GraphqlId(row.get(i)),
            TypeIdentifier::UUID      => PrismaValue::Uuid(row.get(i)),
            TypeIdentifier::Int       => PrismaValue::Int(row.get(i)),
            TypeIdentifier::Boolean   => PrismaValue::Boolean(row.get(i)),
            TypeIdentifier::Enum      => PrismaValue::Enum(row.get(i)),
            TypeIdentifier::Json      => PrismaValue::Json(row.get(i)),
            TypeIdentifier::DateTime  => PrismaValue::DateTime(row.get(i)),
            TypeIdentifier::Relation  => panic!("We should not have a Relation here!"),
            TypeIdentifier::Float => {
                let v: f64 = row.get(i);
                PrismaValue::Float(v as f32)
            }
        }
    }

    fn table_location(database: &str, table: &str) -> String {
        format!("{}.{}", database, table)
    }
}

impl Connector for Sqlite {
    fn get_node_by_where(
        &self,
        database_name: &str,
        selector: &NodeSelector,
    ) -> PrismaResult<Vec<PrismaValue>> {
        self.with_connection(database_name, |conn| {
            let select_fields: &[&Field] = selector.selected_fields;
            let field_names: Vec<&str> = select_fields
                .iter()
                .map(|field| field.name.as_ref())
                .collect();
            let table_location = Self::table_location(database_name, selector.model.name.as_ref());

            let query = dbg!(select_from(&table_location)
                .columns(field_names.as_slice())
                .so_that(selector.field.name.equals(DatabaseValue::Parameter))
                .compile()
                .unwrap());

            let params = vec![(selector.value as &ToSql)];
            let mut result = Vec::new();

            let query_result = conn.query_row(&query, params.as_slice(), |row| {
                for (i, field) in selector.model.fields.iter().enumerate() {
                    result.push(Self::fetch_value(field.type_identifier, row, i));
                }
            });

            match query_result {
                Err(RusqlError::QueryReturnedNoRows) => Ok(result),
                Err(error) => Err(Error::from(error)),
                Ok(_) => Ok(result)
            }
        })
    }
}

impl ToSql for PrismaValue {
    fn to_sql(&self) -> Result<ToSqlOutput, RusqlError> {
        let value = match self {
            PrismaValue::String(value)   => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Enum(value)     => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Json(value)     => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Uuid(value)     => ToSqlOutput::from(value.as_ref() as &str),
            PrismaValue::Float(value)    => ToSqlOutput::from(*value as f64),
            PrismaValue::Int(value)      => ToSqlOutput::from(*value),
            PrismaValue::Boolean(value)  => ToSqlOutput::from(*value),
            PrismaValue::DateTime(value) => value.to_sql().unwrap(),
            PrismaValue::Null(_)         => ToSqlOutput::from(Null),

            PrismaValue::GraphqlId(value) => match value.id_value {
                Some(IdValue::String(ref value)) => ToSqlOutput::from(value.as_ref() as &str),
                Some(IdValue::Int(value))        => ToSqlOutput::from(value),
                None                             => panic!("We got an empty ID value here. Tsk tsk.")
            },

            PrismaValue::Relation(_) => panic!("We should not have a Relation value here."),
        };

        Ok(value)
    }
}

impl FromSql for GraphqlId {
    fn column_result(value: ValueRef<'_>) -> FromSqlResult<Self> {
        value.as_str()
            .map(|strval| {
                GraphqlId{ id_value: Some(IdValue::String(strval.to_string())) }
            })
            .or_else(|_| value.as_i64().map(|intval| {
                GraphqlId{ id_value: Some(IdValue::Int(intval)) }
            }))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{connector::Connector, schema::*};

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
                 CREATE TABLE graphcool.user(id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT);
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
                type_identifier: TypeIdentifier::GraphQLID,
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
            name: String::from("user"),
            stable_identifier: String::from("user"),
            is_embedded: false,
            fields: fields,
        };

        let find_by = PrismaValue::String(String::from("Musti"));
        let scalars = model.scalar_fields();

        let selector = NodeSelector::new(
            db_name,
            &model,
            &model.fields[1],
            &find_by,
            &scalars
        );

        let result = sqlite.get_node_by_where(db_name, &selector).unwrap();

        assert_eq!(2, result.len());
        assert_eq!(PrismaValue::GraphqlId(GraphqlId { id_value: Some(IdValue::Int(1)) }), result[0]);
        assert_eq!(PrismaValue::String(String::from("Musti")), result[1]);
    }
}
