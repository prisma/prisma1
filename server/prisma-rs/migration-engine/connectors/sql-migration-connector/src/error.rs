use failure::{Fail, Error};
use migration_connector::ConnectorError;

pub type SqlResult<T> = Result<T, SqlError>;

#[derive(Debug, Fail)]
pub enum SqlError {
    #[fail(display = "{}", _0)]
    Generic(String),
    #[fail(display = "Error connecting to the database {}", _0)]
    ConnectionError(&'static str),
    #[fail(display = "Error querying the database: {}", _0)]
    QueryError(Error),
}

impl From<SqlError> for ConnectorError {
    fn from(error: SqlError) -> Self {
        ConnectorError::QueryError(error.into())
    }
}

impl From<prisma_query::error::Error> for SqlError {
    fn from(error: prisma_query::error::Error) -> Self {
        SqlError::QueryError(error.into())
    }
}

impl From<String> for SqlError {
    fn from(error: String) -> Self {
        SqlError::Generic(error)
    }
}

impl From<url::ParseError> for SqlError {
    fn from(_: url::ParseError) -> Self {
        SqlError::ConnectionError("Couldn't parse the connection string.")
    }
}
