use crate::{DatabaseWrite, Sqlite, TransactionalExecutor};
use connector::{mutaction::*, ConnectorResult, DatabaseMutactionExecutor};
use prisma_models::*;
use serde_json::Value;

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

    fn execute(
        &self,
        db_name: String,
        mutaction: DatabaseMutaction,
        parent_id: Option<GraphqlId>, // TODO: we don't need this when we handle the whole mutaction in here.
    ) -> ConnectorResult<DatabaseMutactionResults> {
        self.with_transaction(&db_name, |conn| {
            let mut results = DatabaseMutactionResults::default();

            match mutaction {
                DatabaseMutaction::TopLevel(tlm) => results.merge(Self::execute_toplevel(conn, tlm)?),
                DatabaseMutaction::Nested(nm) => results.merge(Self::execute_nested(conn, nm, parent_id.unwrap())?),
            }

            Ok(results)
        })
    }
}
