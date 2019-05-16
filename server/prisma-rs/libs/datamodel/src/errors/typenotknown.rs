use super::ErrorWithSpan;
use crate::ast::Span;

#[derive(Debug, Clone)]
pub struct TypeNotFoundError {
    pub type_name: String,
    pub span: Span,
}

impl TypeNotFoundError {
    pub fn new(type_name: &str, span: &Span) -> TypeNotFoundError {
        TypeNotFoundError {
            type_name: String::from(type_name),
            span: span.clone(),
        }
    }
}

impl ErrorWithSpan for TypeNotFoundError {
    fn span(&self) -> Span {
        self.span
    }
}

impl std::fmt::Display for TypeNotFoundError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "Type {} is neither a built-in type, nor refers to another model or enum.", self.type_name)
    }
}