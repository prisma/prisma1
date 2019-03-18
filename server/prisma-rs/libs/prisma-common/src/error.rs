use std::{error::Error as StdError, borrow::Cow};
use uuid;

type Cause = Box<dyn StdError>;

#[derive(Debug)]
pub enum Error {
    /// Error forming a connection. Check the connection options and network
    /// status.
    ConnectionError(&'static str, Option<Cause>),
    /// Error querying the database. Check the query format and parameters.
    QueryError(&'static str, Option<Cause>),
    /// Couldn't read the protobuf from Scala
    ProtobufDecodeError(&'static str, Option<Cause>),
    /// Couldn't read the JSON from Scala
    JsonDecodeError(&'static str, Option<Cause>),
    /// Input from Scala was not good
    InvalidInputError(String, Option<Cause>),
    /// Invalid connection arguments, e.g. first and last were both defined in a query
    InvalidConnectionArguments(&'static str),
    /// No result returned from query
    NoResultError,
    /// Configuration error
    ConfigurationError(String),
    /// IO error
    IOError(String),
}

impl std::fmt::Display for Error {
    fn fmt(&self, fmt: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(fmt, "{}", self.description())
    }
}

impl Error {
    fn fetch_cause(opt_cause: &Option<Cause>) -> Option<&(dyn std::error::Error + 'static)> {
        opt_cause.as_ref().map(|cause| &**cause)
    }
}

impl StdError for Error {
    fn description(&self) -> &str {
        match self {
            Error::ConnectionError(message, _) => message,
            Error::QueryError(message, _) => message,
            Error::ProtobufDecodeError(message, _) => message,
            Error::JsonDecodeError(message, _) => message,
            Error::InvalidInputError(message, _) => message,
            Error::InvalidConnectionArguments(message) => message,
            Error::NoResultError => "Query returned no results",
            Error::ConfigurationError(message) => message,
            Error::IOError(message) => message,
        }
    }

    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Error::ConnectionError(_, cause) => Self::fetch_cause(&cause),
            Error::QueryError(_, cause) => Self::fetch_cause(&cause),
            Error::ProtobufDecodeError(_, cause) => Self::fetch_cause(&cause),
            Error::JsonDecodeError(_, cause) => Self::fetch_cause(&cause),
            Error::InvalidInputError(_, cause) => Self::fetch_cause(&cause),
            _ => None,
        }
    }
}

impl From<r2d2::Error> for Error {
    fn from(e: r2d2::Error) -> Error {
        Error::ConnectionError("Error creating database connection", Some(Box::new(e)))
    }
}

impl From<rusqlite::Error> for Error {
    fn from(e: rusqlite::Error) -> Error {
        match e {
            rusqlite::Error::QueryReturnedNoRows => Error::NoResultError,
            _ => Error::QueryError("Error querying SQLite database", Some(Box::new(e))),
        }
    }
}

impl From<prost::DecodeError> for Error {
    fn from(e: prost::DecodeError) -> Error {
        Error::ProtobufDecodeError("Error decoding protobuf message", Some(Box::new(e)))
    }
}

impl From<serde_json::error::Error> for Error {
    fn from(e: serde_json::error::Error) -> Error {
        Error::JsonDecodeError("Error decoding JSON message", Some(Box::new(e)))
    }
}

impl From<uuid::parser::ParseError> for Error {
    fn from(e: uuid::parser::ParseError) -> Error {
        Error::InvalidInputError(String::from("Expected database value to be a UUID, but couldn't parse the value into one."), Some(Box::new(e)))
    }
}

impl From<std::io::Error> for Error {
    fn from(e: std::io::Error) -> Error {
        Error::IOError(format!("IO error: {}", e.description()))
    }
}
