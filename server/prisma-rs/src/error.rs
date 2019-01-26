use std::error::Error as StdError;

type Cause = Box<StdError + Send + Sync>;

#[derive(Debug)]
pub enum Error {
    /// Error forming a connection. Check the connection options and network
    /// status.
    ConnectionError(&'static str, Option<Cause>),
    /// Error querying the database. Check the query format and parameters.
    QueryError(&'static str, Option<Cause>),
    /// No results with the given query.
    NoResultsError,
}

impl std::fmt::Display for Error {
    fn fmt(&self, fmt: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(fmt, "{}", self.description())
    }
}

impl StdError for Error {
    fn description(&self) -> &str {
        match self {
            Error::ConnectionError(message, _) => message,
            Error::QueryError(message, _) => message,
            Error::NoResultsError => "No results"
        }
    }

    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Error::ConnectionError(_, cause) => {
                cause.as_ref().map(|cause| &**cause as &StdError)
            }
            Error::QueryError(_, cause) => {
                cause.as_ref().map(|cause| &**cause as &StdError)
            }
            _ => None,
        }
    }
}

impl From<r2d2::Error> for Error {
    fn from(e: r2d2::Error) -> Error {
        Error::ConnectionError(
            "Error creating database connection",
            Some(Box::new(e)),
        )
    }
}

impl From<rusqlite::Error> for Error {
    fn from(e: rusqlite::Error) -> Error {
        Error::QueryError(
            "Error querying SQLite database",
            Some(Box::new(e)),
        )
    }
}
