use crate::{query_builder::QueryBuilder, DatabaseExecutor};
use chrono::{DateTime, Utc};
use connector::*;
use prisma_models::prelude::*;
use prisma_query::{
    ast::*,
    visitor::{self, *},
};
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::{Row, Transaction, NO_PARAMS};
use serde_json::Value;
use std::{collections::HashSet, env, sync::Arc};
use uuid::Uuid;

type Connection = r2d2::PooledConnection<SqliteConnectionManager>;
type Pool = r2d2::Pool<SqliteConnectionManager>;

pub struct Sqlite {
    pool: Pool,
    test_mode: bool,
}

impl DatabaseExecutor for Sqlite {
    fn with_rows<F, T>(&self, query: Select, db_name: String, f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
    {
        self.with_transaction(&db_name, |conn| Self::query(conn, query, f))
    }
}

impl DatabaseMutactionExecutor for Sqlite {
    fn execute_raw(&self, _query: String) -> ConnectorResult<Value> {
        // self.sqlite.with_connection(&db_name, |conn| {
        //     let res = conn
        //         .prepare(&query)?
        //         .query_map(&params, |row| f(row))?
        //         .map(|row_res| row_res.unwrap())
        //         .collect();

        //     Ok(res)
        // });
        Ok(Value::String("hello world!".to_string()))
    }

    fn execute_create(&self, db_name: String, mutaction: &CreateNode) -> ConnectorResult<GraphqlId> {
        self.with_transaction(&db_name, |conn| {
            let (insert, returned_id) =
                MutationBuilder::create_node(mutaction.model.clone(), mutaction.non_list_args.clone());

            Self::execute_one(conn, insert)?;

            let id = match returned_id {
                Some(id) => id,
                None => GraphqlId::Int(conn.last_insert_rowid() as usize),
            };

            for (field_name, list_value) in mutaction.list_args.clone() {
                let field = mutaction.model.fields().find_from_scalar(&field_name).unwrap();
                let table = field.scalar_list_table();
                let inserts = MutationBuilder::create_scalar_list_value(table, &list_value, &id);

                Self::execute_many(conn, inserts)?;
            }

            Ok(id)
        })
    }

    fn execute_update(&self, db_name: String, mutaction: &UpdateNode) -> ConnectorResult<GraphqlId> {
        self.with_transaction(&db_name, |conn| {
            let model = mutaction.where_.field.model();

            let id: GraphqlId = {
                let (_, select) = {
                    let selected_fields = SelectedFields::from(model.fields().id());
                    QueryBuilder::get_node_by_where(&mutaction.where_, &selected_fields)
                };

                let ids = Self::query(conn, select, |row| {
                    let id: GraphqlId = row.get(0);
                    Ok(id)
                })?;

                let opt_id = ids.into_iter().next();

                opt_id.ok_or_else(|| ConnectorError::NodeNotFoundForWhere {
                    field: mutaction.where_.field.name.clone(),
                    value: mutaction.where_.value.clone(),
                })?
            };

            if let Some(update) = MutationBuilder::update_node_by_id(Arc::clone(&model), &id, &mutaction.non_list_args)?
            {
                Self::execute_one(conn, update)?;
            }

            for (field_name, list_value) in mutaction.list_args.clone() {
                let field = model.fields().find_from_scalar(&field_name).unwrap();
                let table = field.scalar_list_table();
                let (delete, inserts) = MutationBuilder::update_scalar_list_value(table, &list_value, &id);

                Self::execute_one(conn, delete)?;
                Self::execute_many(conn, inserts)?;
            }

            Ok(id)
        })
    }

    fn execute_delete(&self, _db_name: String, _mutaction: &DeleteNode) -> ConnectorResult<SingleNode> {
        unimplemented!()
        /*
         *
        self.with_transaction(&db_name, |conn| {
            let model = mutaction.where_.field.model();

            let node: SingleNode = {
                let selected_fields = SelectedFields::from(model.fields().scalar());
                let scalar_fields = selected_fields.scalar_non_list();
                let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();

                let (_, select) = QueryBuilder::get_node_by_where(&mutaction.where_, &selected_fields);

                let nodes = Self::query(conn, select, |row| Ok(Self::read_row(row, &selected_fields)?))?;

                let opt_node = nodes.into_iter().next().map(|node| SingleNode { node, field_names });

                opt_node.ok_or_else(|| ConnectorError::NodeNotFoundForWhere {
                    field: mutaction.where_.field.name.clone(),
                    value: mutaction.where_.value.clone(),
                })?
            };

            let id = node.get_id_value(Arc::clone(&model))?;
        })
         */
    }
}

impl Sqlite {
    /// Creates a new SQLite pool connected into local memory. By querying from
    /// different databases, it will try to create them to
    /// `$SERVER_ROOT/db/db_name` if they do not exists yet.
    pub fn new(connection_limit: u32, test_mode: bool) -> ConnectorResult<Sqlite> {
        let pool = r2d2::Pool::builder()
            .max_size(connection_limit)
            .build(SqliteConnectionManager::memory())?;

        Ok(Sqlite { pool, test_mode })
    }

    fn query<F, T>(conn: &Transaction, query: Select, mut f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
    {
        let (query_sql, params) = dbg!(visitor::Sqlite::build(query));

        let res: ConnectorResult<Vec<T>> = conn
            .prepare(&query_sql)?
            .query_map(&params, |row| f(row))?
            .map(|row_res| row_res.unwrap())
            .collect();

        Ok(res?)
    }

    fn execute_one<T>(conn: &Transaction, query: T) -> ConnectorResult<()>
    where
        T: Into<Query>,
    {
        let (sql, params) = dbg!(visitor::Sqlite::build(query));
        conn.prepare(&sql)?.execute(&params)?;

        Ok(())
    }

    fn execute_many<T>(conn: &Transaction, queries: Vec<T>) -> ConnectorResult<()>
    where
        T: Into<Query>,
    {
        for query in queries {
            Self::execute_one(conn, query)?;
        }

        Ok(())
    }

    /// Will create a new file if it doesn't exist. Otherwise loads db/db_name
    /// from the SERVER_ROOT.
    fn attach_database(conn: &mut Connection, db_name: &str) -> ConnectorResult<()> {
        let mut stmt = dbg!(conn.prepare("PRAGMA database_list")?);

        let databases: HashSet<String> = stmt
            .query_map(NO_PARAMS, |row| {
                let name: String = row.get(1);
                name
            })?
            .map(|res| res.unwrap())
            .collect();

        // FIXME(Dom): Correct config for sqlite
        let server_root = env::var("SERVER_ROOT").unwrap_or_else(|_| String::from("."));

        if !databases.contains(db_name) {
            let path = dbg!(format!("{}/db/{}.db", server_root, db_name));
            dbg!(conn.execute("ATTACH DATABASE ? AS ?", &[path.as_ref(), db_name])?);
        }

        dbg!(conn.execute("PRAGMA foreign_keys = ON", NO_PARAMS)?);

        Ok(())
    }

    pub fn fetch_int(row: &Row) -> i64 {
        row.get_checked(0).unwrap_or(0)
    }

    pub fn read_row(row: &Row, selected_fields: &SelectedFields) -> ConnectorResult<Node> {
        let mut fields = Vec::new();

        for (i, sf) in selected_fields.scalar_non_list().iter().enumerate() {
            fields.push(Self::fetch_value(sf.type_identifier, &row, i)?);
        }

        Ok(Node::new(fields))
    }

    /// Converter function to wrap the limited set of types in SQLite to a
    /// richer PrismaValue.
    pub fn fetch_value(typ: TypeIdentifier, row: &Row, i: usize) -> ConnectorResult<PrismaValue> {
        let result = match typ {
            TypeIdentifier::String => row.get_checked(i).map(|val| PrismaValue::String(val)),
            TypeIdentifier::GraphQLID => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
            TypeIdentifier::UUID => {
                let result: Result<String, rusqlite::Error> = row.get_checked(i);

                if let Ok(val) = result {
                    let uuid = Uuid::parse_str(val.as_ref())?;
                    Ok(PrismaValue::Uuid(uuid))
                } else {
                    result.map(|s| PrismaValue::String(s))
                }
            }
            TypeIdentifier::Int => row.get_checked(i).map(|val| PrismaValue::Int(val)),
            TypeIdentifier::Boolean => row.get_checked(i).map(|val| PrismaValue::Boolean(val)),
            TypeIdentifier::Enum => row.get_checked(i).map(|val| PrismaValue::Enum(val)),
            TypeIdentifier::Json => row.get_checked(i).map(|val| PrismaValue::Json(val)),
            TypeIdentifier::DateTime => row.get_checked(i).map(|ts: i64| {
                let nsecs = ((ts % 1000) * 1_000_000) as u32;
                let secs = (ts / 1000) as i64;
                let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
                let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

                PrismaValue::DateTime(datetime)
            }),
            TypeIdentifier::Relation => panic!("We should not have a Relation here!"),
            TypeIdentifier::Float => row.get_checked(i).map(|val: f64| PrismaValue::Float(val)),
        };

        match result {
            Err(rusqlite::Error::InvalidColumnType(_, rusqlite::types::Type::Null)) => Ok(PrismaValue::Null),
            Ok(pv) => Ok(pv),
            Err(e) => Err(e.into()),
        }
    }

    pub fn with_connection<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Connection) -> ConnectorResult<T>,
    {
        let mut conn = self.pool.get()?;
        Self::attach_database(&mut conn, db_name)?;

        let result = f(&mut conn);

        if self.test_mode {
            dbg!(conn.execute("DETACH DATABASE ?", &[db_name])?);
        }

        result
    }

    /// Take a new connection from the pool and create the database if it
    /// doesn't exist yet.
    pub fn with_transaction<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&Transaction) -> ConnectorResult<T>,
    {
        self.with_connection(db_name, |conn| {
            let tx = conn.transaction()?;
            let result = f(&tx);

            tx.commit()?;
            result
        })
    }
}
