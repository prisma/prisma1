use super::{DirectiveValidationError, ErrorWithSpan, TypeNotFoundError};

#[derive(Debug)]
pub struct ErrorCollection {
    pub errors: Vec<Box<ErrorWithSpan>>,
}

impl ErrorCollection {
    pub fn new() -> ErrorCollection {
        ErrorCollection { errors: Vec::new() }
    }

    pub fn push(&mut self, err: Box<ErrorWithSpan>) {
        self.errors.push(err)
    }

    pub fn has_errors(&self) -> bool {
        self.errors.len() > 0
    }

    pub fn to_iter(&self) -> std::slice::Iter<Box<ErrorWithSpan>> {
        self.errors.iter()
    }

    pub fn append(&mut self, errs: &mut ErrorCollection) {
        self.errors.append(&mut errs.errors)
    }
}

impl std::fmt::Display for ErrorCollection {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{:?}", self.errors)
    }
}

impl std::convert::From<DirectiveValidationError> for ErrorCollection {
    fn from(error: DirectiveValidationError) -> Self {
        let mut col = ErrorCollection::new();
        col.push(Box::new(error));
        col
    }
}

impl std::convert::From<TypeNotFoundError> for ErrorCollection {
    fn from(error: TypeNotFoundError) -> Self {
        let mut col = ErrorCollection::new();
        col.push(Box::new(error));
        col
    }
}
