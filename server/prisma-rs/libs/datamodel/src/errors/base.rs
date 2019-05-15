use crate::ast::Span;
use std;

// We do not implement the Error trait. It is not needed here,
// as parser errors should be handled and parsed differently than
// conventional errors.

pub trait ErrorWithSpan: std::fmt::Display + std::fmt::Debug {
    fn span(&self) -> Span;
}
