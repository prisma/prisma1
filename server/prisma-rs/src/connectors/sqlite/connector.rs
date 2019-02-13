use crate::{
    connectors::{Connector, Sqlite},
    models::{Model, Renameable, ScalarField},
    protobuf::prisma::{Node, QueryArguments, ValueContainer},
    PrismaResult, PrismaValue,
};

use rusqlite::{types::ToSql, NO_PARAMS};

use sql::prelude::*;

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

            let field_names: Vec<&str> = selected_fields
                .iter()
                .map(|field| field.db_name())
                .collect();

            let table_location = Self::table_location(database_name, model.db_name());
            let query = dbg!(select_from(&table_location)
                .columns(&field_names)
                .so_that(field.db_name().equals(DatabaseValue::Parameter))
                .compile()
                .unwrap());

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
        database_name: &str,
        model: &Model,
        selected_fields: &[&ScalarField],
        query_arguments: QueryArguments,
    ) -> PrismaResult<Vec<Node>> {
        self.with_connection(database_name, |conn| {
            let field_names: Vec<&str> = selected_fields
                .iter()
                .map(|field| field.db_name())
                .collect();

            let table_location = Self::table_location(database_name, model.db_name());

            let conditions: ConditionTree = query_arguments
                .filter
                .map(|filter| filter.into())
                .unwrap_or(ConditionTree::NoCondition);

            let query = dbg!(select_from(&table_location)
                .columns(&field_names)
                .so_that(conditions)
                .compile()
                .unwrap());

            let mut stmt = conn.prepare(&query).unwrap();

            let nodes_iter = stmt.query_map(NO_PARAMS, |row| {
                let mut values = Vec::new();

                for (i, field) in selected_fields.iter().enumerate() {
                    let prisma_value = Some(Self::fetch_value(field.type_identifier, &row, i));
                    values.push(ValueContainer { prisma_value });
                }

                Node { values }
            })?;

            let mut nodes = Vec::new();
            for node in nodes_iter {
                nodes.push(node?);
            }

            Ok(dbg!(nodes))
        })
    }
}
