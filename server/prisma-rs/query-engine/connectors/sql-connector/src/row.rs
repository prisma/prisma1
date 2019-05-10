use connector::ConnectorResult;
use prisma_models::{Node, PrismaValue, TypeIdentifier};

/// An allocated representation of a `Row` returned from the database.
#[derive(Debug, Clone, Default)]
pub struct PrismaRow {
    pub values: Vec<PrismaValue>,
}

impl From<PrismaRow> for Node {
    fn from(row: PrismaRow) -> Node {
        Node::new(row.values)
    }
}

pub trait ToPrismaRow {
    /// Conversion from a database specific row to an allocated `PrismaRow`. To
    /// help deciding the right types, the provided `TypeIdentifier`s should map
    /// to the returned columns in the right order.
    fn to_prisma_row<'b, T>(&'b self, idents: T) -> ConnectorResult<PrismaRow>
    where
        T: IntoIterator<Item = &'b TypeIdentifier>;
}
