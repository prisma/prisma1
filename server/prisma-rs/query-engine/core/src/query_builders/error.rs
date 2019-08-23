use crate::{query_document::QueryValue, schema::InputType};
use std::fmt;

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
    pub fn format(&self, ident: usize) -> String {
        match self {
            QueryValidationError::AssertionError(reason) => format!("General assertion error: {}.", reason),
            QueryValidationError::RequiredValueNotSetError => "A value is required but not set.".into(),
            QueryValidationError::FieldNotFoundError => "Field does not exist on enclosing type.".into(),
            QueryValidationError::ArgumentNotFoundError => "Argument does not exist on enclosing type.".into(),
            QueryValidationError::AtLeastOneSelectionError => "At least one selection is required.".into(),
            QueryValidationError::ValueParseError(reason) => format!("Error parsing value: {}.", reason),
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

impl From<prisma_models::DomainError> for QueryValidationError {
    fn from(err: prisma_models::DomainError) -> Self {
        QueryValidationError::AssertionError(format!("Domain error occurred: {}", err))
    }
}
