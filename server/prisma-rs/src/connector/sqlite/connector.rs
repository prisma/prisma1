use crate::{
    connector::{sqlite::SqliteQueryBuilder, Connector, Sqlite},
    models::{Field, Model, Renameable, ScalarField},
    protobuf::prisma::{Node, QueryArguments, ValueContainer},
    query_builder::QueryBuilder,
    PrismaResult, PrismaValue,
};

use rusqlite::types::ToSql;

impl Connector for Sqlite {
    fn get_node_by_where(
        &self,
        database_name: &str,
        model: &Model,
        selected_fields: &[&ScalarField],
        query_conditions: (&ScalarField, &PrismaValue),
    ) -> PrismaResult<Node> {
        self.with_connection(database_name, |conn| {
            let (field, condition) = query_conditions;
            let params = vec![(condition as &ToSql)];
            let mut values = Vec::new();

            let query = SqliteQueryBuilder::get_node_by_where(
                database_name,
                model.db_name(),
                selected_fields,
                field,
            );

            conn.query_row(&query, params.as_slice(), |row| {
                for (i, field) in selected_fields.iter().enumerate() {
                    let prisma_value = Some(Self::fetch_value(field.type_identifier, row, i));
                    values.push(ValueContainer { prisma_value });
                }
            })?;

            Ok(Node { values })
        })
    }

    fn get_nodes(
        &self,
        _database_name: &str,
        _table_name: &str,
        _selected_fields: &[Field],
        _query_arguments: &QueryArguments,
    ) -> PrismaResult<Vec<Node>> {
        Ok(Vec::new())
    }
}
