use crate::{
    data_resolvers::{DataResolver, SelectQuery, Sqlite},
    protobuf::prelude::*,
};

use prisma_common::PrismaResult;
use prisma_query::visitor::{self, Visitor};

impl DataResolver for Sqlite {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<(Vec<Node>, Vec<String>)> {
        let db_name = query.db_name;
        let query_ast = query.query_ast;
        let fields = query.selected_fields.fields;
        let field_names = query.selected_fields.names;

        self.with_connection(&db_name, |conn| {
            let (query_sql, params) = dbg!(visitor::Sqlite::build(query_ast));
            let mut stmt = conn.prepare(&query_sql)?;

            let nodes_iter = stmt.query_map(&params, |row| {
                let mut values = Vec::new();

                for (i, field) in fields.iter().enumerate() {
                    let prisma_value = Some(Self::fetch_value(field.type_identifier, &row, i));
                    values.push(ValueContainer { prisma_value });
                }

                Node { values }
            })?;

            let mut nodes = Vec::new();
            for node in nodes_iter {
                nodes.push(node?);
            }

            Ok(dbg!((nodes, field_names)))
        })
    }
}
