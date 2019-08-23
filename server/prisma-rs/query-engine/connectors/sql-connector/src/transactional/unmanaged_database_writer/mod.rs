mod create;
mod delete;
mod delete_many;
mod nested;
mod relation;
mod update;
mod update_many;

use crate::{
    database::{SqlCapabilities, SqlDatabase},
    error::SqlError,
    query_builder::WriteQueryBuilder,
    RawQuery, Transaction, Transactional,
};
use connector_interface::{self, result_ast::*, write_ast::*, UnmanagedDatabaseWriter};
use serde_json::Value;
use std::sync::Arc;

impl<T> UnmanagedDatabaseWriter for SqlDatabase<T>
where
    T: Transactional + SqlCapabilities,
{
    fn execute(&self, db_name: String, write_query: RootWriteQuery) -> connector_interface::Result<WriteQueryResult> {
        let result = self.executor.with_transaction(&db_name, |conn| {
            fn create(conn: &mut dyn Transaction, cn: &CreateRecord) -> crate::Result<WriteQueryResult> {
                let parent_id = create::execute(conn, Arc::clone(&cn.model), &cn.non_list_args, &cn.list_args)?;
                nested::execute(conn, &cn.nested_writes, &parent_id)?;

                Ok(WriteQueryResult {
                    identifier: Identifier::Id(parent_id),
                    typ: WriteQueryResultType::Create,
                })
            }

            fn update(conn: &mut dyn Transaction, un: &UpdateRecord) -> crate::Result<WriteQueryResult> {
                let parent_id = update::execute(conn, &un.where_, &un.non_list_args, &un.list_args)?;
                nested::execute(conn, &un.nested_writes, &parent_id)?;

                Ok(WriteQueryResult {
                    identifier: Identifier::Id(parent_id),
                    typ: WriteQueryResultType::Update,
                })
            }

            match write_query {
                RootWriteQuery::CreateRecord(ref cn) => Ok(create(conn, cn)?),
                RootWriteQuery::UpdateRecord(ref un) => Ok(update(conn, un)?),
                RootWriteQuery::UpsertRecord(ref ups) => match conn.find_id(&ups.where_) {
                    Err(_e @ SqlError::RecordNotFoundForWhere { .. }) => Ok(create(conn, &ups.create)?),
                    Err(e) => Err(e.into()),
                    Ok(_) => Ok(update(conn, &ups.update)?),
                },
                RootWriteQuery::UpdateManyRecords(ref uns) => {
                    let count = update_many::execute(
                        conn,
                        Arc::clone(&uns.model),
                        &uns.filter,
                        &uns.non_list_args,
                        &uns.list_args,
                    )?;

                    Ok(WriteQueryResult {
                        identifier: Identifier::Count(count),
                        typ: WriteQueryResultType::Many,
                    })
                }
                RootWriteQuery::DeleteRecord(ref dn) => {
                    let record = delete::execute(conn, &dn.where_)?;

                    Ok(WriteQueryResult {
                        identifier: Identifier::Record(record),
                        typ: WriteQueryResultType::Delete,
                    })
                }
                RootWriteQuery::DeleteManyRecords(ref dns) => {
                    let count = delete_many::execute(conn, Arc::clone(&dns.model), &dns.filter)?;

                    Ok(WriteQueryResult {
                        identifier: Identifier::Count(count),
                        typ: WriteQueryResultType::Many,
                    })
                }
                RootWriteQuery::ResetData(ref rd) => {
                    let tables = WriteQueryBuilder::truncate_tables(Arc::clone(&rd.internal_data_model));
                    conn.empty_tables(tables)?;

                    Ok(WriteQueryResult {
                        identifier: Identifier::None,
                        typ: WriteQueryResultType::Unit,
                    })
                }
            }
        })?;

        Ok(result)
    }

    fn execute_raw(&self, db_name: String, query: String) -> connector_interface::Result<Value> {
        let result = self
            .executor
            .with_transaction(&db_name, |conn| conn.raw_json(RawQuery::from(query)))?;

        Ok(result)
    }
}
