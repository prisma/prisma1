use super::DataResolver;
use crate::protobuf::prelude::*;
use crate::{database_executor::DatabaseExecutor, node_selector::NodeSelector, query_builder::QueryBuilder};
use chrono::{DateTime, Utc};
use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use rusqlite::Row;

pub struct SqlResolver<T>
where
    T: DatabaseExecutor,
{
    database_executor: T,
}

impl<T> DataResolver for SqlResolver<T>
where
    T: DatabaseExecutor,
{
    fn get_node_by_where(
        &self,
        node_selector: NodeSelector,
        selected_fields: SelectedFields,
    ) -> PrismaResult<Option<SingleNode>> {
        let (db_name, query) = QueryBuilder::get_node_by_where(node_selector, &selected_fields);
        let scalar_fields = selected_fields.scalar_non_list();
        let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();

        let nodes = self
            .database_executor
            .with_rows(query, db_name, |row| Self::read_row(row, &selected_fields))?;

        let result = nodes.into_iter().next().map(|node| SingleNode { node, field_names });
        Ok(result)
    }

    fn get_nodes(
        &self,
        model: ModelRef,
        query_arguments: QueryArguments,
        selected_fields: SelectedFields,
    ) -> PrismaResult<ManyNodes> {
        let scalar_fields = selected_fields.scalar_non_list();
        let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();
        let (db_name, query) = QueryBuilder::get_nodes(model, query_arguments, &selected_fields);

        let nodes = self
            .database_executor
            .with_rows(query, db_name, |row| Self::read_row(row, &selected_fields))?;

        let result = ManyNodes { nodes, field_names };
        Ok(result)
    }
}

impl<T> SqlResolver<T>
where
    T: DatabaseExecutor,
{
    pub fn new(database_executor: T) -> Self {
        Self { database_executor }
    }

    fn read_row(row: &Row, selected_fields: &SelectedFields) -> Node {
        let fields = selected_fields
            .scalar_non_list()
            .iter()
            .enumerate()
            .map(|(i, sf)| Self::fetch_value(sf.type_identifier, &row, i))
            .collect();

        Node::new(fields)
    }

    /// Converter function to wrap the limited set of types in SQLite to a
    /// richer PrismaValue.
    fn fetch_value(typ: TypeIdentifier, row: &Row, i: usize) -> PrismaValue {
        let result = match typ {
            TypeIdentifier::String => row.get_checked(i).map(|val| PrismaValue::String(val)),
            TypeIdentifier::GraphQLID => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
            TypeIdentifier::UUID => row.get_checked(i).map(|val| PrismaValue::Uuid(val)),
            TypeIdentifier::Int => row.get_checked(i).map(|val| PrismaValue::Int(val)),
            TypeIdentifier::Boolean => row.get_checked(i).map(|val| PrismaValue::Boolean(val)),
            TypeIdentifier::Enum => row.get_checked(i).map(|val| PrismaValue::Enum(val)),
            TypeIdentifier::Json => row.get_checked(i).map(|val| PrismaValue::Json(val)),
            TypeIdentifier::DateTime => row.get_checked(i).map(|ts: i64| {
                let nsecs = ((ts % 1000) * 1_000_000) as u32;
                let secs = (ts / 1000) as i64;
                let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
                let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

                PrismaValue::DateTime(datetime)
            }),
            TypeIdentifier::Relation => panic!("We should not have a Relation here!"),
            TypeIdentifier::Float => row.get_checked(i).map(|val: f64| PrismaValue::Float(val)),
        };

        result.unwrap_or_else(|e| match e {
            rusqlite::Error::InvalidColumnType(_, rusqlite::types::Type::Null) => PrismaValue::Null,
            _ => panic!(e),
        })
    }
}
