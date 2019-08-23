use super::ValidationError;

/// Represents a list of validation or parser errors.
///
/// This is uses to accumulate all errors and show them all at once.
#[derive(Debug, Clone)]
pub struct ErrorCollection {
    pub errors: Vec<ValidationError>,
}

impl ErrorCollection {
    /// Creates a new, empty error collection.
    pub fn new() -> ErrorCollection {
        ErrorCollection { errors: Vec::new() }
    }

    /// Adds an error.
    pub fn push(&mut self, err: ValidationError) {
        self.errors.push(err)
    }

    /// Returns true, if there is at least one error
    /// in this collection.
    pub fn has_errors(&self) -> bool {
        self.errors.len() > 0
    }

    /// Creates an iterator over all errors in this collection.
    pub fn to_iter(&self) -> std::slice::Iter<ValidationError> {
        self.errors.iter()
    }

    /// Appends all errors from another collection to this collection.
    pub fn append(&mut self, errs: &mut ErrorCollection) {
        self.errors.append(&mut errs.errors)
    }

    pub fn ok(&self) -> Result<(), ErrorCollection> {
        if self.has_errors() {
            Err(self.clone())
        } else {
            Ok(())
        }
    }
}

impl std::fmt::Display for ErrorCollection {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{:?}", self.errors)
    }
}

impl std::convert::From<ValidationError> for ErrorCollection {
    fn from(error: ValidationError) -> Self {
        let mut col = ErrorCollection::new();
        col.push(error);
        col
    }
}
