use crate::ScalarField;
use prisma_query::ast::*;

#[derive(Debug, Clone)]
pub struct ScalarListTable<'a> {
    parent_field: &'a ScalarField,
    table_name: String,
}

// Todo Unsure about node -> record rename here
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

    pub fn table(&self) -> Table<'static> {
        let internal_data_model = self.parent_field.internal_data_model();
        let database_name = internal_data_model.db_name.clone();

        Table::from((database_name, self.table_name.clone()))
    }

    pub fn node_id_column(&self) -> Column<'static> {
        Column::from(Self::NODE_ID_FIELD_NAME).table(self.table())
    }

    pub fn position_column(&self) -> Column<'static> {
        Column::from(Self::POSITION_FIELD_NAME).table(self.table())
    }

    pub fn value_column(&self) -> Column<'static> {
        Column::from(Self::VALUE_FIELD_NAME).table(self.table())
    }
}
