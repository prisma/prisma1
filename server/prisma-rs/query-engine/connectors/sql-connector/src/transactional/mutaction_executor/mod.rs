mod create;
mod delete;
mod delete_many;
mod nested;
mod relation;
mod update;
mod update_many;

use crate::{database::SqlDatabase, error::SqlError, RawQuery, SqlResult, Transaction, Transactional};
use connector::{mutaction::*, ConnectorResult, DatabaseMutactionExecutor};
use serde_json::Value;
use std::sync::Arc;

impl<T> DatabaseMutactionExecutor for SqlDatabase<T>
where
    T: Transactional,
{
    fn execute(
        &self,
        db_name: String,
        mutaction: TopLevelDatabaseMutaction,
    ) -> ConnectorResult<DatabaseMutactionResult> {
        let result = self.executor.with_transaction(&db_name, |conn: &mut Transaction| {
            fn create(conn: &mut Transaction, cn: &CreateRecord) -> SqlResult<DatabaseMutactionResult> {
                let parent_id = create::execute(conn, Arc::clone(&cn.model), &cn.non_list_args, &cn.list_args)?;
                nested::execute(conn, &cn.nested_mutactions, &parent_id)?;

                Ok(DatabaseMutactionResult {
                    identifier: Identifier::Id(parent_id),
                    typ: DatabaseMutactionResultType::Create,
                })
            }

            fn update(conn: &mut Transaction, un: &UpdateRecord) -> SqlResult<DatabaseMutactionResult> {
                let parent_id = update::execute(conn, &un.where_, &un.non_list_args, &un.list_args)?;
                nested::execute(conn, &un.nested_mutactions, &parent_id)?;

                Ok(DatabaseMutactionResult {
                    identifier: Identifier::Id(parent_id),
                    typ: DatabaseMutactionResultType::Update,
                })
            }

            match mutaction {
                TopLevelDatabaseMutaction::CreateRecord(ref cn) => Ok(create(conn, cn)?),
                TopLevelDatabaseMutaction::UpdateRecord(ref un) => Ok(update(conn, un)?),
                TopLevelDatabaseMutaction::UpsertRecord(ref ups) => match conn.find_id(&ups.where_) {
                    Err(_e @ SqlError::RecordNotFoundForWhere { .. }) => Ok(create(conn, &ups.create)?),
                    Err(e) => return Err(e.into()),
                    Ok(_) => Ok(update(conn, &ups.update)?),
                },
                TopLevelDatabaseMutaction::UpdateManyRecords(ref uns) => {
                    let count = update_many::execute(
                        conn,
                        Arc::clone(&uns.model),
                        &uns.filter,
                        &uns.non_list_args,
                        &uns.list_args,
                    )?;

                    Ok(DatabaseMutactionResult {
                        identifier: Identifier::Count(count),
                        typ: DatabaseMutactionResultType::Many,
                    })
                }
                TopLevelDatabaseMutaction::DeleteRecord(ref dn) => {
                    let record = delete::execute(conn, &dn.where_)?;

                    Ok(DatabaseMutactionResult {
                        identifier: Identifier::Record(record),
                        typ: DatabaseMutactionResultType::Delete,
                    })
                }
                TopLevelDatabaseMutaction::DeleteManyRecords(ref dns) => {
                    let count = delete_many::execute(conn, Arc::clone(&dns.model), &dns.filter)?;

                    Ok(DatabaseMutactionResult {
                        identifier: Identifier::Count(count),
                        typ: DatabaseMutactionResultType::Many,
                    })
                }
                TopLevelDatabaseMutaction::ResetData(ref rd) => {
                    conn.truncate(Arc::clone(&rd.internal_data_model))?;

                    Ok(DatabaseMutactionResult {
                        identifier: Identifier::None,
                        typ: DatabaseMutactionResultType::Unit,
                    })
                }
            }
        })?;

        Ok(result)
    }

    fn execute_raw(&self, db_name: String, query: String) -> ConnectorResult<Value> {
        let result = self
            .executor
            .with_transaction(&db_name, |conn: &mut Transaction| conn.raw(RawQuery::from(query)))?;

        Ok(result)
    }
}
