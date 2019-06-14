///! Experimental visitor. WIP.
use super::*;

/// Defines operations possible with visitors.
pub enum VisitorOperation<T> {
    Replace(T),
    Remove,
    None,
}

pub trait SchemaAstVisitor {
    fn visit_output_type(&self, output_type: &OutputType) -> VisitorOperation<OutputType>;
    fn visit_input_type(&self, input_type: &InputType) -> VisitorOperation<InputType>;
}
