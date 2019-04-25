use connector::ConnectorResult;
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::Transaction;

pub type Connection = r2d2::PooledConnection<SqliteConnectionManager>;

pub trait TransactionalExecutor {
    /// Takes a new connection and starts a transaction, that is commited if the
    /// given `F` was successful. Attaches any databases, if not yet in memory.
    ///
    /// [with_connection](struct.Sqlite.html#methid.with_connection) should be
    /// used if atomicity is not needed.
    /// ```rust
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use connector::{error::ConnectorError, ConnectorResult};
    /// # use sqlite_connector::*;
    /// # use prisma_query::ast::*;
    /// # let sqlite = Sqlite::new("../".into(), 1, false).unwrap();
    /// let _ = sqlite.with_transaction("test", |trans| {
    ///     trans.execute(
    ///         "CREATE TABLE IF NOT EXISTS test.users (id Text, name Text);",
    ///         NO_PARAMS
    ///     ).unwrap();
    ///
    ///     Ok(())
    /// });
    ///
    /// let _: ConnectorResult<()> = sqlite.with_transaction("test", |trans| {
    ///     trans.execute(
    ///         "INSERT INTO test.users (id, name) VALUES ('id1', 'John')",
    ///         NO_PARAMS,
    ///     ).unwrap();
    ///
    ///     Err(ConnectorError::RelationViolation {
    ///         relation_name: String::from("Cats"),
    ///         model_a_name: String::from("A"),
    ///         model_b_name: String::from("B"),
    ///     })
    /// });
    ///
    /// let count: i64 = sqlite.with_connection("test", |conn| {
    ///     let res = conn.query_row("SELECT COUNT(id) FROM test.users", NO_PARAMS, |row| {
    ///         row.get_checked(0).unwrap_or(0)
    ///     })?;
    ///
    ///     Ok(res)
    /// }).unwrap();
    ///
    /// assert_eq!(0, count);
    /// ```
    fn with_transaction<F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&Transaction) -> ConnectorResult<T>;

    /// Takes a new connection and if needed attaches the database if needed.
    ///
    /// [with_transaction](struct.Sqlite.html#method.with_transaction) should be
    /// used if atomicity is needed.
    fn with_connection<'a, F, T>(&self, db_name: &str, f: F) -> ConnectorResult<T>
    where
        F: FnOnce(&mut Connection) -> ConnectorResult<T>;
}
