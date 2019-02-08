use crate::{
    models::{ScalarField, Renameable},
    query_builder::QueryBuilder,
};

use sql::{grammar::operation::eq::Equable, prelude::*};

pub struct SqliteQueryBuilder;

impl SqliteQueryBuilder {
    /// Helper to namespace different databases.
    fn table_location(database: &str, table: &str) -> String {
        format!("{}.{}", database, table)
    }
}

impl QueryBuilder for SqliteQueryBuilder {
    fn get_node_by_where(
        database_name: &str,
        table_name: &str,
        selected_fields: &[&ScalarField],
        query_field: &ScalarField,
    ) -> String {
        let table_location = Self::table_location(database_name, table_name);
        let field_names: Vec<&str> = selected_fields
            .iter()
            .map(|field| field.db_name())
            .collect();

        select_from(&table_location)
            .columns(&field_names)
            .so_that(query_field.db_name().equals(DatabaseValue::Parameter))
            .compile()
            .unwrap()
    }
}
