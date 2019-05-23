use crate::{relational::TableRelationInfo, Connection, SqlError};
use prisma_query::ast::*;

/// Contains several helpers which work
/// for databases that support the information_schema table.

fn query_tables(connection: &mut Connection, schema: &str) -> Vec<String> {
    let query = Select::from_table("information_schema.tables")
        // Needed because of different capitalization conventions between DBs
        .column(Column::from("table_name").alias("table_name"))
        .so_that(ConditionTree::and(
            "table_schema".equals(schema),
            "table_type".equals("BASE_TABLE"),
        ))
        .order_by("table_name");

    // Return table names as Vec
    unimplemented!("Unimplemented")
}

fn query_relations(connection: &mut Connection, schema: &str) -> Vec<TableRelationInfo> {
    let join_1 = Table::from("information_schema.key_column_usage")
        .alias("keyColumn1")
        .on(ConditionTree::and(
            ConditionTree::and(
                ("keyColumn1", "constraint_catalog").equals(Column::from(("refConstraints", "constraint_catalog"))),
                ("keyColumn1", "constraint_schema").equals(Column::from(("refConstraints", "constraint_schema"))),
            ),
            ("keyColumn1", "constraint_name").equals(Column::from(("refConstraints", "constraint_name"))),
        ));
    let join_2 = Table::from("information_schema.key_column_usage")
        .alias("keyColumn1")
        .on(ConditionTree::and(
            ConditionTree::and(
                ("keyColumn2", "constraint_catalog").equals(Column::from(("refConstraints", "constraint_catalog"))),
                ("keyColumn2", "constraint_schema").equals(Column::from(("refConstraints", "constraint_schema"))),
            ),
            ConditionTree::and(
                ("keyColumn2", "constraint_name").equals(Column::from(("refConstraints", "constraint_name"))),
                ("keyColumn2", "ordinal_position").equals(Column::from(("keyColumn1", "ordinal_position"))),
            ),
        ));

    let query = Select::from_table(Table::from("information_schema.referential_constraints ").alias("refConstraints"))
        .column(Column::from("keyColumn1.constraint_name").alias("fkConstraintName"))
        .column(Column::from("keyColumn1.table_name").alias("fkTableName"))
        .column(Column::from("keyColumn1.column_name").alias("fkColumnName"))
        .column(Column::from("keyColumn2.constraint_name").alias("referencedConstraintName"))
        .column(Column::from("keyColumn2.table_name").alias("referencedTableName"))
        .column(Column::from("keyColumn2.column_name").alias("referencedColumnName"))
        .inner_join(join_1)
        .inner_join(join_2)
        .so_that(("refConstraints", "constraint_schema").equals(schema));

    // Map to table relation info and return.

    unimplemented!("Unimplemented")
}

fn list_schemas(connection: &mut Connection) -> Result<Vec<String>, SqlError> {
    let query = Select::from_table("information_schema.schemata")
        .column("schema_name")
        .so_that("schema_name".not_like("information_schema"));

    let res = connection.query(Query::from(query))?;

    Ok(res.iter().map(|row| String::from(row.as_str(0).unwrap())).collect())
}
