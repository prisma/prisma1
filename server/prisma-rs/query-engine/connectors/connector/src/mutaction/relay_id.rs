use prisma_query::ast::{Column, Insert, Table};

pub struct RelayId<'a> {
    database_name: &'a str,
}

impl<'a> RelayId<'a> {
    const TABLE_NAME: &'static str = "_RealayId";
    const ID: &'static str = "id";
    const STABLE_MODEL_IDENTIFIER: &'static str = "stableModelIdentifier";

    pub fn new(database_name: &'a str) -> Self {
        Self { database_name }
    }

    pub fn create(&self, stable_identifier: &str) -> Insert {
        Insert::single_into(self.table())
            .value(self.stable_identifier_column(), stable_identifier)
            .into()
    }

    pub fn id_column(&self) -> Column {
        Column::from(Self::ID).table(self.table())
    }

    fn table(&self) -> Table {
        Table::from((self.database_name, Self::TABLE_NAME))
    }

    fn stable_identifier_column(&self) -> Column {
        Column::from(Self::STABLE_MODEL_IDENTIFIER).table(self.table())
    }
}
