use crate::data_resolvers::{DataResolver, SelectQuery, Sqlite};

use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::visitor::{self, Visitor};

impl DataResolver for Sqlite {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<(Vec<Vec<PrismaValue>>, Vec<String>)> {
        let db_name = query.db_name;
        let query_ast = query.query_ast;
        let fields = query.selected_fields.fields;
        let field_names = query.selected_fields.names;

        self.with_connection(&db_name, |conn| {
            let (query_sql, params) = dbg!(visitor::Sqlite::build(query_ast));
            let mut stmt = conn.prepare(&query_sql)?;

            let nodes_iter = stmt.query_map(&params, |row| {
                fields
                    .iter()
                    .enumerate()
                    .map(|(i, field)| Self::fetch_value(field.type_identifier, &row, i))
                    .collect()
            })?;

            let mut nodes = Vec::new();
            for node in nodes_iter {
                nodes.push(node?);
            }

            Ok(dbg!((nodes, field_names)))
        })
    }
}
