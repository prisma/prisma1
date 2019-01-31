use crate::{
    schema::{Field, Model},
    PrismaValue,
};

/// A helper struct for selecting data.
pub struct NodeSelector<'a> {
    /// The model to select from
    pub model: &'a Model,
    /// The table to look into
    pub table: String,
    /// The name of the field to filtering
    pub field: &'a Field,
    /// The value of the field, should be in the corresponding type.
    pub value: &'a PrismaValue,
    /// Fields to select from the table
    pub selected_fields: &'a [Field],
}

impl<'a> NodeSelector<'a> {
    #[allow(dead_code)]
    pub fn new(
        database: &'a str,
        model: &'a Model,
        field: &'a Field,
        value: &'a PrismaValue,
        selected_fields: &'a [Field],
    ) -> NodeSelector<'a> {
        let table = format!("{}.{}", database, model.stable_identifier);

        NodeSelector {
            model,
            table,
            field,
            value,
            selected_fields,
        }
    }
}
