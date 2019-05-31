use connector::error::*;
use failure::{Error, Fail};
use prisma_models::prelude::DomainError;
use std::string::FromUtf8Error;

#[cfg(feature = "sqlite")]
use rusqlite;

#[cfg(feature = "sqlite")]
use libsqlite3_sys as ffi;

#[derive(Debug, Fail)]
pub enum SqlError {
    #[fail(display = "Unique constraint failed: {}", field_name)]
    UniqueConstraintViolation { field_name: String },

    #[fail(display = "Null constraint failed: {}", field_name)]
    NullConstraintViolation { field_name: String },

    #[fail(display = "Node does not exist.")]
    NodeDoesNotExist,

    #[fail(display = "Column does not exist")]
    ColumnDoesNotExist,

    #[fail(display = "Error creating a database connection.")]
    ConnectionError(Error),

    #[fail(display = "Error querying the database: {}", _0)]
    QueryError(Error),

    #[fail(display = "The provided arguments are not supported.")]
    InvalidConnectionArguments,

    #[fail(display = "The column value was different from the model")]
    ColumnReadFailure(Error),

    #[fail(display = "Field cannot be null: {}", field)]
    FieldCannotBeNull { field: String },

    #[fail(display = "{}", _0)]
    DomainError(DomainError),

    #[fail(display = "Node not found: {}", _0)]
    NodeNotFoundForWhere(NodeSelectorInfo),

    #[fail(
        display = "Violating a relation {} between {} and {}",
        relation_name, model_a_name, model_b_name
    )]
    RelationViolation {
        relation_name: String,
        model_a_name: String,
        model_b_name: String,
    },

    #[fail(
        display = "The relation {} has no node for the model {} connected to a Node for the model {} on your mutation path.",
        relation_name, parent_name, child_name
    )]
    NodesNotConnected {
        relation_name: String,
        parent_name: String,
        parent_where: Option<NodeSelectorInfo>,
        child_name: String,
        child_where: Option<NodeSelectorInfo>,
    },

    #[fail(display = "Conversion error: {}", _0)]
    ConversionError(Error),

    #[fail(display = "Database creation error: {}", _0)]
    DatabaseCreationError(&'static str),
}

impl From<SqlError> for ConnectorError {
    fn from(sql: SqlError) -> Self {
        match sql {
            SqlError::UniqueConstraintViolation { field_name } => {
                ConnectorError::UniqueConstraintViolation { field_name }
            }
            SqlError::NullConstraintViolation { field_name } => ConnectorError::NullConstraintViolation { field_name },
            SqlError::NodeDoesNotExist => ConnectorError::NodeDoesNotExist,
            SqlError::ColumnDoesNotExist => ConnectorError::ColumnDoesNotExist,
            SqlError::ConnectionError(e) => ConnectorError::ConnectionError(e),
            SqlError::InvalidConnectionArguments => ConnectorError::InvalidConnectionArguments,
            SqlError::ColumnReadFailure(e) => ConnectorError::ColumnReadFailure(e),
            SqlError::FieldCannotBeNull { field } => ConnectorError::FieldCannotBeNull { field },
            SqlError::DomainError(e) => ConnectorError::DomainError(e),
            SqlError::NodeNotFoundForWhere(info) => ConnectorError::NodeNotFoundForWhere(info),
            SqlError::RelationViolation {
                relation_name,
                model_a_name,
                model_b_name,
            } => ConnectorError::RelationViolation {
                relation_name,
                model_a_name,
                model_b_name,
            },
            SqlError::NodesNotConnected {
                relation_name,
                parent_name,
                parent_where,
                child_name,
                child_where,
            } => ConnectorError::NodesNotConnected {
                relation_name,
                parent_name,
                parent_where,
                child_name,
                child_where,
            },
            SqlError::ConversionError(e) => ConnectorError::ConversionError(e),
            SqlError::DatabaseCreationError(e) => ConnectorError::DatabaseCreationError(e),
            SqlError::QueryError(e) => ConnectorError::QueryError(e),
        }
    }
}

impl From<DomainError> for SqlError {
    fn from(e: DomainError) -> SqlError {
        SqlError::DomainError(e)
    }
}

impl From<serde_json::error::Error> for SqlError {
    fn from(e: serde_json::error::Error) -> SqlError {
        SqlError::ConversionError(e.into())
    }
}

impl From<r2d2::Error> for SqlError {
    fn from(e: r2d2::Error) -> SqlError {
        SqlError::ConnectionError(e.into())
    }
}

#[cfg(feature = "sqlite")]
impl From<rusqlite::Error> for SqlError {
    fn from(e: rusqlite::Error) -> SqlError {
        match e {
            rusqlite::Error::QueryReturnedNoRows => SqlError::NodeDoesNotExist,

            rusqlite::Error::SqliteFailure(
                ffi::Error {
                    code: ffi::ErrorCode::ConstraintViolation,
                    extended_code: 2067,
                },
                Some(description),
            ) => {
                let splitted: Vec<&str> = description.split(": ").collect();
                let splitted: Vec<&str> = splitted[1].split(".").collect();

                SqlError::UniqueConstraintViolation {
                    field_name: splitted[1].into(),
                }
            }

            rusqlite::Error::SqliteFailure(
                ffi::Error {
                    code: ffi::ErrorCode::ConstraintViolation,
                    extended_code: 1555,
                },
                Some(description),
            ) => {
                let splitted: Vec<&str> = description.split(": ").collect();
                let splitted: Vec<&str> = splitted[1].split(".").collect();

                SqlError::UniqueConstraintViolation {
                    field_name: splitted[1].into(),
                }
            }

            rusqlite::Error::SqliteFailure(
                ffi::Error {
                    code: ffi::ErrorCode::ConstraintViolation,
                    extended_code: 1299,
                },
                Some(description),
            ) => {
                let splitted: Vec<&str> = description.split(": ").collect();
                let splitted: Vec<&str> = splitted[1].split(".").collect();

                SqlError::NullConstraintViolation {
                    field_name: splitted[1].into(),
                }
            }

            e => SqlError::QueryError(e.into()),
        }
    }
}

impl From<uuid::parser::ParseError> for SqlError {
    fn from(e: uuid::parser::ParseError) -> SqlError {
        SqlError::ColumnReadFailure(e.into())
    }
}

impl From<uuid::BytesError> for SqlError {
    fn from(e: uuid::BytesError) -> SqlError {
        SqlError::ColumnReadFailure(e.into())
    }
}

impl From<FromUtf8Error> for SqlError {
    fn from(e: FromUtf8Error) -> SqlError {
        SqlError::ColumnReadFailure(e.into())
    }
}

#[cfg(feature = "postgresql")]
impl From<tokio_postgres::error::Error> for SqlError {
    fn from(e: tokio_postgres::error::Error) -> SqlError {
        use tokio_postgres::error::DbError;

        match e.code().map(|c| c.code()) {
            // Don't look at me, I'm hideous ;((
            Some("23505") => {
                let error = e.into_source().unwrap(); // boom
                let db_error = error.downcast_ref::<DbError>().unwrap(); // BOOM
                let detail = db_error.detail().unwrap(); // KA-BOOM

                let splitted: Vec<&str> = detail.split(")=(").collect();
                let splitted: Vec<&str> = splitted[0].split(" (").collect();
                let field_name = splitted[1].replace("\"", "");

                SqlError::UniqueConstraintViolation { field_name }
            }
            // Even lipstick will not save this...
            Some("23502") => {
                let error = e.into_source().unwrap(); // boom
                let db_error = error.downcast_ref::<DbError>().unwrap(); // BOOM
                let detail = db_error.detail().unwrap(); // KA-BOOM

                let splitted: Vec<&str> = detail.split(")=(").collect();
                let splitted: Vec<&str> = splitted[0].split(" (").collect();
                let field_name = splitted[1].replace("\"", "");

                SqlError::NullConstraintViolation { field_name }
            }
            _ => SqlError::QueryError(e.into()),
        }
    }
}

#[cfg(feature = "postgresql")]
impl From<native_tls::Error> for SqlError {
    fn from(e: native_tls::Error) -> SqlError {
        SqlError::ConnectionError(e.into())
    }
}

#[cfg(feature = "mysql")]
impl From<mysql_client::error::Error> for SqlError {
    fn from(e: mysql_client::error::Error) -> SqlError {
        use mysql_client::error::Error;
        use mysql_client::error::MySqlError;

        match e {
            Error::MySqlError(MySqlError {
                state: _,
                ref message,
                code,
            }) if code == 1062 => {
                let splitted: Vec<&str> = message.split_whitespace().collect();
                let splitted: Vec<&str> = splitted.last().map(|s| s.split("'").collect()).unwrap();
                let splitted: Vec<&str> = splitted[1].split("_").collect();

                let field_name: String = splitted[0].into();

                SqlError::UniqueConstraintViolation { field_name }
            }
            Error::MySqlError(MySqlError {
                state: _,
                ref message,
                code,
            }) if code == 1263 => {
                let splitted: Vec<&str> = message.split_whitespace().collect();
                let splitted: Vec<&str> = splitted.last().map(|s| s.split("'").collect()).unwrap();
                let splitted: Vec<&str> = splitted[1].split("_").collect();

                let field_name: String = splitted[0].into();

                SqlError::NullConstraintViolation { field_name }
            }
            e => SqlError::QueryError(e.into()),
        }
    }
}

#[cfg(feature = "mysql")]
impl From<mysql_client::FromValueError> for SqlError {
    fn from(e: mysql_client::FromValueError) -> SqlError {
        SqlError::ColumnReadFailure(e.into())
    }
}
