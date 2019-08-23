use failure::{Error, Fail};

#[derive(Debug, Fail)]
pub enum ConnectorError {
    #[fail(display = "{}", _0)]
    Generic(Error),

    #[fail(display = "Error querying the database: {}", _0)]
    QueryError(Error),
}

impl From<prisma_query::error::Error> for ConnectorError {
    fn from(e: prisma_query::error::Error) -> Self {
        ConnectorError::QueryError(e.into())
    }
}
