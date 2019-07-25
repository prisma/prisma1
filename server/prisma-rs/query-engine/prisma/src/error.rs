use core::{response_ir, CoreError};
use datamodel::errors::ErrorCollection;
use failure::{Error, Fail};
use graphql_parser::query::ParseError as GqlParseError;
use prisma_common::error::CommonError;
use serde_json;

#[cfg(feature = "sql")]
use sql_connector::SqlError;

#[derive(Debug, Fail)]
pub enum PrismaError {
    #[fail(display = "{}", _0)]
    QueryValidationError(String),

    #[fail(display = "{}", _0)]
    SerializationError(String),

    #[fail(display = "{}", _0)]
    CoreError(CoreError),

    #[fail(display = "{}", _0)]
    JsonDecodeError(Error),

    #[fail(display = "{}", _0)]
    ConfigurationError(String),

    #[fail(display = "{}", _0)]
    ConversionError(ErrorCollection, String),

    #[fail(display = "{}", _0)]
    IOError(Error),

    #[fail(display = "{}", _0)]
    InvocationError(String),

    /// (Feature name, additional error text)
    #[fail(display = "Unsupported feature: {}. {}", _0, _1)]
    UnsupportedFeatureError(&'static str, String),
}

/// Helps to handle gracefully handle errors as a response.
impl Into<response_ir::Response> for PrismaError {
    fn into(self) -> response_ir::Response {
        let error_msg = format!("{}", self);
        response_ir::Response::Error(error_msg)
    }
}

/// Pretty print helper errors.
pub trait PrettyPrint {
    fn pretty_print(&self);
}

impl PrettyPrint for PrismaError {
    fn pretty_print(&self) {
        match self {
            PrismaError::ConversionError(errors, dml_string) => {
                for error in errors.to_iter() {
                    println!("");
                    error
                        .pretty_print(&mut std::io::stderr().lock(), "data model, line", &dml_string)
                        .expect("Failed to write errors to stderr");
                }
            }
            x => println!("{}", x),
        };
    }
}

impl From<CoreError> for PrismaError {
    fn from(e: CoreError) -> PrismaError {
        PrismaError::CoreError(e)
    }
}

impl From<url::ParseError> for PrismaError {
    fn from(e: url::ParseError) -> PrismaError {
        PrismaError::ConfigurationError(format!("Error parsing connection string: {}", e))
    }
}

impl From<CommonError> for PrismaError {
    fn from(e: CommonError) -> PrismaError {
        PrismaError::ConfigurationError(format!("{}", e))
    }
}

impl From<serde_json::error::Error> for PrismaError {
    fn from(e: serde_json::error::Error) -> PrismaError {
        PrismaError::JsonDecodeError(e.into())
    }
}

impl From<std::io::Error> for PrismaError {
    fn from(e: std::io::Error) -> PrismaError {
        PrismaError::IOError(e.into())
    }
}

impl From<std::string::FromUtf8Error> for PrismaError {
    fn from(e: std::string::FromUtf8Error) -> PrismaError {
        PrismaError::IOError(e.into())
    }
}

impl From<base64::DecodeError> for PrismaError {
    fn from(e: base64::DecodeError) -> PrismaError {
        PrismaError::ConfigurationError(format!("Invalid base64: {}", e))
    }
}

impl From<GqlParseError> for PrismaError {
    fn from(e: GqlParseError) -> PrismaError {
        PrismaError::QueryValidationError(format!("Error parsing GraphQL query: {}", e))
    }
}

#[cfg(feature = "sql")]
impl From<SqlError> for PrismaError {
    fn from(e: SqlError) -> PrismaError {
        PrismaError::ConfigurationError(format!("{}", e))
    }
}
