use crate::{query_document::QueryValue, InputType};
use connector::error::ConnectorError;
use failure::Fail;
use prisma_models::DomainError;
use std::fmt;

#[derive(Debug, Fail)]
pub enum CoreError {
    #[fail(display = "Error in connector: {}", _0)]
    ConnectorError(ConnectorError),

    #[fail(display = "Error in domain logic: {}", _0)]
    DomainError(DomainError),

    #[fail(display = "{}", _0)]
    QueryValidationError(QueryValidationError),

    #[fail(display = "{}", _0)]
    LegacyQueryValidationError(String),

    #[fail(display = "Unsupported feature: {}", _0)]
    UnsupportedFeatureError(String),
}

impl From<ConnectorError> for CoreError {
    fn from(e: ConnectorError) -> CoreError {
        CoreError::ConnectorError(e)
    }
}

impl From<DomainError> for CoreError {
    fn from(e: DomainError) -> CoreError {
        CoreError::DomainError(e)
    }
}

#[derive(Debug)]
pub enum QueryValidationError {
    AssertionError(String), // Naming is WIP. Denotes a generic validation error.
    RequiredValueNotSetError,
    FieldNotFoundError,
    ArgumentNotFoundError,
    AtLeastOneSelectionError,
    ValueParseError(String),
    // InputFieldValidationError,
    ValueTypeMismatchError {
        have: QueryValue,
        want: InputType,
    },
    FieldValidationError {
        field_name: String,
        inner: Box<QueryValidationError>,
    },
    ArgumentValidationError {
        argument: String,
        inner: Box<QueryValidationError>,
    },
    ObjectValidationError {
        object_name: String,
        inner: Box<QueryValidationError>,
    },
}

impl QueryValidationError {
    fn format(&self, ident: usize) -> String {
        match self {
            QueryValidationError::AssertionError(reason) => format!("General assertion error: {}.", reason),
            QueryValidationError::RequiredValueNotSetError => "A value is required but not set.".into(),
            QueryValidationError::FieldNotFoundError => "Field does not exist on enclosing type.".into(),
            QueryValidationError::ArgumentNotFoundError => "Argument does not exist on enclosing type.".into(),
            QueryValidationError::AtLeastOneSelectionError => "At least one selection is required.".into(),
            QueryValidationError::ValueParseError(reason) => format!("Error parsing value: {}.", reason),
            // QueryValidationError::InputFieldValidationError => unimplemented!(),
            QueryValidationError::ValueTypeMismatchError { have, want } => {
                format!("Value types mismatch. Have: {:?}, want: {:?}", have, want)
            } // wip value/type formatting

            // Validation root
            QueryValidationError::ObjectValidationError { object_name, inner } => format!(
                "{} (object)\n{}",
                object_name,
                Self::ident(inner.format(ident + 2), ident + 2)
            ),

            QueryValidationError::FieldValidationError { field_name, inner } => format!(
                "{} (field)\n{}",
                field_name,
                Self::ident(inner.format(ident + 2), ident + 2)
            ),
            QueryValidationError::ArgumentValidationError { argument, inner } => format!(
                "{} (argument)\n{}",
                argument,
                Self::ident(inner.format(ident + 2), ident + 2)
            ),
            // _ => unimplemented!(),
        }
    }

    fn ident(s: String, size: usize) -> String {
        format!("{}↳ {}", " ".repeat(size), s)
    }
}

impl fmt::Display for QueryValidationError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "Error occurred during query validation & transformation:\n{}",
            self.format(0)
        )
    }
}

impl From<QueryValidationError> for CoreError {
    fn from(e: QueryValidationError) -> CoreError {
        CoreError::QueryValidationError(e)
    }
}
