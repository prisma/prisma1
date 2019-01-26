use std::error::Error as StdError;

#[derive(Debug)]
pub enum Error {
    ConnectionError(Option<String>),
    QueryError(Option<String>),
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
            Error::ConnectionError(opt_m) => match opt_m {
                Some(message) => &message,
                None => "Connection error when trying to get a connection to the database."
            }
            Error::QueryError(opt_m) => match opt_m {
                Some(message) => &message,
                None => "Error querying the database."
            }
            Error::NoResultsError => "No results"
        }
    }

    fn cause(&self) -> Option<&std::error::Error> {
        None
    }
}

impl From<r2d2::Error> for Error {
    fn from(e: r2d2::Error) -> Error {
        Error::ConnectionError(Some(format!("Error forming a connection: {}", e)))
    }
}

impl From<rusqlite::Error> for Error {
    fn from(e:rusqlite::Error) -> Error {
        Error::QueryError(Some(format!("Error querying SQLite database: {}", e)))
    }
}
