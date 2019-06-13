
use migration_connector::ConnectorError;

pub type SqlResult<T> = Result<T, SqlError>;

#[derive(Debug)]
pub enum SqlError {
    Generic(String)
}

impl From<SqlError> for ConnectorError {
    fn from(error: SqlError) -> Self {
        ConnectorError::Generic(format!("{:?}", error))
    }
}

impl From<prisma_query::error::Error> for SqlError {
    fn from(error: prisma_query::error::Error) -> Self {
        SqlError::Generic(format!("{:?}", error))
    }
}