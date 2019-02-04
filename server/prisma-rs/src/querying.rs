use crate::{
    schema::{ScalarField, Model},
    PrismaValue,
};

/// A helper struct for selecting data.
pub struct NodeSelector<'a> {
    /// The model to select from
    pub model: &'a Model,
    /// The name of the field to filtering
    pub field: &'a ScalarField,
    /// The value of the field, should be in the corresponding type.
    pub value: &'a PrismaValue,
    /// Fields to select from the table
    pub selected_fields: &'a [&'a ScalarField],
}

impl<'a> NodeSelector<'a> {
    pub fn new(
        model: &'a Model,
        field: &'a ScalarField,
        value: &'a PrismaValue,
        selected_fields: &'a [&'a ScalarField],
    ) -> NodeSelector<'a> {
        NodeSelector {
            model,
            field,
            value,
            selected_fields,
        }
    }
}
