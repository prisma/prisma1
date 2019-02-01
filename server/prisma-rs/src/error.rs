use std::error::Error as StdError;
use crate::protobuf::prisma;

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
    InvalidInputError(String),
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
            Error::ConnectionError(message, _)     => message,
            Error::QueryError(message, _)          => message,
            Error::ProtobufDecodeError(message, _) => message,
            Error::JsonDecodeError(message, _)     => message,
            Error::InvalidInputError(message)      => message,
        }
    }

    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Error::ConnectionError(_, cause)     => Self::fetch_cause(&cause),
            Error::QueryError(_, cause)          => Self::fetch_cause(&cause),
            Error::ProtobufDecodeError(_, cause) => Self::fetch_cause(&cause),
            Error::JsonDecodeError(_, cause)     => Self::fetch_cause(&cause),
            _ => None,
        }
    }
}

impl Into<prisma::error::Value> for Error {
    fn into(self) -> prisma::error::Value {
        match self {
            Error::ConnectionError(message, _)     => prisma::error::Value::ConnectionError(message.to_string()),
            Error::QueryError(message, _)          => prisma::error::Value::QueryError(message.to_string()),
            Error::ProtobufDecodeError(message, _) => prisma::error::Value::ProtobufDecodeError(message.to_string()),
            Error::JsonDecodeError(message, _)     => prisma::error::Value::JsonDecodeError(message.to_string()),
            Error::InvalidInputError(message)      => prisma::error::Value::InvalidInputError(message.to_string()),
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
        Error::QueryError("Error querying SQLite database", Some(Box::new(e)))
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
