use crate::{
    mutaction::{MutationBuilder, NestedActions},
    DatabaseCreate, DatabaseRead, DatabaseWrite, Sqlite,
};
use connector::ConnectorResult;
use prisma_models::{GraphqlId, ModelRef, PrismaArgs, PrismaListValue, RelationFieldRef};
use rusqlite::Transaction;
use std::sync::Arc;

impl DatabaseCreate for Sqlite {
    fn execute_create<T>(
        conn: &Transaction,
        model: ModelRef,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>
    where
        T: AsRef<str>,
    {
        let (insert, returned_id) = MutationBuilder::create_node(Arc::clone(&model), non_list_args.clone());

        Self::execute_one(conn, insert)?;

        let id = match returned_id {
            Some(id) => id,
            None => GraphqlId::Int(conn.last_insert_rowid() as usize),
        };

        for (field_name, list_value) in list_args {
            let field = model.fields().find_from_scalar(field_name.as_ref()).unwrap();
            let table = field.scalar_list_table();

            if let Some(insert) = MutationBuilder::create_scalar_list_value(table.table(), &list_value, &id) {
                Self::execute_one(conn, insert)?;
            }
        }

        Ok(id)
    }

    fn execute_nested_create<T>(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>
    where
        T: AsRef<str>,
    {
        if let Some((select, check)) = actions.required_check(parent_id)? {
            let ids = Self::query(conn, select, Self::fetch_id)?;
            check.call_box(ids.into_iter().next())?
        };

        if let Some(query) = actions.parent_removal(parent_id) {
            Self::execute_one(conn, query)?;
        }

        let related_field = relation_field.related_field();

        if related_field.relation_is_inlined_in_parent() {
            let mut prisma_args = non_list_args.clone();
            prisma_args.insert(related_field.name.clone(), parent_id.clone());

            Self::execute_create(conn, relation_field.related_model(), &prisma_args, list_args)
        } else {
            let id = Self::execute_create(conn, relation_field.related_model(), non_list_args, list_args)?;
            let relation_query = MutationBuilder::create_relation(relation_field, parent_id, &id);

            Self::execute_one(conn, relation_query)?;

            Ok(id)
        }
    }
}
