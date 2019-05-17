use super::errors::ValidationError;

#[derive(Debug)]
pub struct ErrorCollection {
    pub errors: Vec<ValidationError>,
}

impl ErrorCollection {
    pub fn new() -> ErrorCollection {
        ErrorCollection { errors: Vec::new() }
    }

    pub fn push(&mut self, err: ValidationError) {
        self.errors.push(err)
    }

    pub fn has_errors(&self) -> bool {
        self.errors.len() > 0
    }

    pub fn to_iter(&self) -> std::slice::Iter<ValidationError> {
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

impl std::convert::From<ValidationError> for ErrorCollection {
    fn from(error: ValidationError) -> Self {
        let mut col = ErrorCollection::new();
        col.push(error);
        col
    }
}
