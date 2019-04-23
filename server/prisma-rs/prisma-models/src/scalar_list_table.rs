use crate::ScalarField;
use prisma_query::ast::*;

#[derive(Debug, Clone)]
pub struct ScalarListTable<'a> {
    parent_field: &'a ScalarField,
    table_name: String,
}

impl<'a> ScalarListTable<'a> {
    pub const NODE_ID_FIELD_NAME: &'static str = "nodeId";
    pub const POSITION_FIELD_NAME: &'static str = "position";
    pub const VALUE_FIELD_NAME: &'static str = "value";

    pub fn new(parent_field: &'a ScalarField) -> Self {
        let table_name = format!("{}_{}", parent_field.model().db_name(), parent_field.db_name());

        Self {
            parent_field,
            table_name,
        }
    }

    pub fn table(&self) -> Table {
        let schema = self.parent_field.schema();
        let database_name = schema.db_name.as_ref();

        Table::from((database_name, self.table_name.as_ref()))
    }

    pub fn node_id_column(&self) -> Column {
        Column::from(Self::NODE_ID_FIELD_NAME).table(self.table())
    }

    pub fn position_column(&self) -> Column {
        Column::from(Self::POSITION_FIELD_NAME).table(self.table())
    }

    pub fn value_column(&self) -> Column {
        Column::from(Self::VALUE_FIELD_NAME).table(self.table())
    }
}
