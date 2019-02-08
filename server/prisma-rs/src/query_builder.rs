use crate::models::ScalarField;

pub trait QueryBuilder {
    fn get_node_by_where(
        database_name: &str,
        table_name: &str,
        selected_fields: &[&ScalarField],
        query_field: &ScalarField,
    ) -> String;
}
