use crate::data_resolvers::*;

use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use prisma_query::visitor::{self, Visitor};

impl DataResolver for Sqlite {
    fn select_nodes(&self, query: SelectQuery) -> PrismaResult<SelectResult> {
        let db_name = query.db_name;
        let query_ast = query.query_ast;
        let selected_fields = query.selected_fields;
        let needs_relation_fields = selected_fields.needs_relation_fields();

        let scalar_fields = selected_fields.scalar_non_list();
        let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();

        self.with_connection(&db_name, |conn| {
            let (query_sql, params) = dbg!(visitor::Sqlite::build(query_ast));
            let mut stmt = conn.prepare(&query_sql)?;

            let nodes_iter = stmt.query_map(&params, |row| {
                let values = scalar_fields
                    .iter()
                    .enumerate()
                    .map(|(i, sf)| Self::fetch_value(sf.type_identifier, &row, i))
                    .collect();

                let mut node = Node::new(values);

                if needs_relation_fields {
                    let position = scalar_fields.len();

                    // TODO: These might crash if the ids are null. Check later if it is so, mkay?
                    node.add_related_id(row.get(position));
                    node.add_parent_id(row.get(position + 1));
                }

                node
            })?;

            let mut nodes = Vec::new();
            for node in nodes_iter {
                nodes.push(node?);
            }

            Ok(dbg!(SelectResult { nodes, field_names }))
        })
    }
}
