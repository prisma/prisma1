use crate::Sqlite;
use connector::{DatabaseMutaction, DatabaseMutactionExecutor, DatabaseMutactionResults};
use prisma_common::PrismaResult;
use serde_json::Value;
use std::sync::Arc;

pub struct SqliteDatabaseMutactionExecutor {
    pub _sqlite: Arc<Sqlite>,
}

impl DatabaseMutactionExecutor for SqliteDatabaseMutactionExecutor {
    fn execute_raw(&self, _query: String) -> Value {
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

    fn execute(&self, mutaction: DatabaseMutaction) -> PrismaResult<DatabaseMutactionResults> {
        dbg!(mutaction);
        Ok(DatabaseMutactionResults { results: vec![] })
        /*
        executor.transaction(|tx| {
            let id = self.steps.into_iter().fold(None, |acc, step| {
                let query = match self.needing {
                    Some(returning) => match &*returning.read() {
                        Returning::Expected => panic!("Needed ID value not set for mutaction"),
                        Returning::Got(id) => match self.query {
                            Query::Insert(insert) => Query::Insert(insert.value(id)),
                            _ => panic!("Only inserts are supported for now"),
                        },
                    },
                    None => query,
                };

                let id: Option<GraphqlId> = executor.with_rows(self.query, db_name, |row| row.get(0));

                if let Some(returning) = self.returning {
                    returning.write().set(id);
                };
            });

            DatabaseMutactionResult(id, mutaction);
        })


        tx.commit()?;
        */
    }
}
