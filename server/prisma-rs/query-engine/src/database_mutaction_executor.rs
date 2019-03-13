use crate::database_executor::Sqlite;
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_raw(&self, query: String) -> Value;
}

pub struct SqliteDatabaseMutactionExecutor {
    // sqlite: Sqlite,
}

impl DatabaseMutactionExecutor for SqliteDatabaseMutactionExecutor {
    fn execute_raw(&self, query: String) -> Value {
        // self.sqlite.with_connection(&db_name, |conn| {
        //     let res = conn
        //         .prepare(&query)?
        //         .query_map(&params, |row| f(row))?
        //         .map(|row_res| row_res.unwrap())
        //         .collect();

        //     Ok(res)
        // });
        Value::String("hello world!".to_string())
    }
}
