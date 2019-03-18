use crate::{database_executor::DatabaseExecutor, query_builder::QueryBuilder, sqlite::Sqlite};
use chrono::{DateTime, Utc};
use connector::{DataResolver, NodeSelector, QueryArguments, ScalarListValues};
use itertools::Itertools;
use prisma_common::PrismaResult;
use prisma_models::prelude::*;
use rusqlite::Row;
use uuid::Uuid;
use std::sync::Arc;

pub struct SqlResolver<T>
where
    T: DatabaseExecutor,
{
    database_executor: Arc<T>,
}

impl<T> SqlResolver<T>
where
    T: DatabaseExecutor,
{
    pub fn new(database_executor: Arc<T>) -> Self {
        Self { database_executor }
    }
}

impl DataResolver for SqlResolver<Sqlite> {
    fn get_node_by_where(
        &self,
        node_selector: &NodeSelector,
        selected_fields: &SelectedFields,
    ) -> PrismaResult<Option<SingleNode>> {
        let (db_name, query) = QueryBuilder::get_node_by_where(node_selector, selected_fields);
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

        Ok(ManyNodes { nodes, field_names })
    }

    fn get_related_nodes(
        &self,
        from_field: RelationFieldRef,
        from_node_ids: &[GraphqlId],
        query_arguments: QueryArguments,
        selected_fields: &SelectedFields,
    ) -> PrismaResult<ManyNodes> {
        let scalar_fields = selected_fields.scalar_non_list();
        let field_names = scalar_fields.iter().map(|f| f.name.clone()).collect();
        let (db_name, query) =
            QueryBuilder::get_related_nodes(from_field, from_node_ids, query_arguments, selected_fields);

        let nodes = self.database_executor.with_rows(query, db_name, |row| {
            let mut node = Self::read_row(row, &selected_fields)?;
            let position = scalar_fields.len();

            node.add_related_id(row.get(position));
            node.add_parent_id(row.get(position + 1));
            Ok(node)
        })?;

        Ok(ManyNodes { nodes, field_names })
    }

    fn count_by_model(&self, model: ModelRef, query_arguments: QueryArguments) -> PrismaResult<usize> {
        let (db_name, query) = QueryBuilder::count_by_model(model, query_arguments);

        let res = self
            .database_executor
            .with_rows(query, db_name, |row| Ok(Self::fetch_int(row)))?
            .into_iter()
            .next()
            .unwrap_or(0);

        Ok(res as usize)
    }

    fn count_by_table(&self, database: &str, table: &str) -> PrismaResult<usize> {
        let query = QueryBuilder::count_by_table(database, table);

        let res = self
            .database_executor
            .with_rows(query, String::from(database), |row| Ok(Self::fetch_int(row)))?
            .into_iter()
            .next()
            .unwrap_or(0);

        Ok(res as usize)
    }

    fn get_scalar_list_values_by_node_ids(
        &self,
        list_field: ScalarFieldRef,
        node_ids: Vec<GraphqlId>,
    ) -> PrismaResult<Vec<ScalarListValues>> {
        let type_identifier = list_field.type_identifier;
        let (db_name, query) = QueryBuilder::get_scalar_list_values_by_node_ids(list_field, node_ids);

        let results = self.database_executor.with_rows(query, db_name, |row| {
            let node_id: GraphqlId = row.get(0);
            let position: u32 = row.get(1);
            let value: PrismaValue = Self::fetch_value(type_identifier, row, 2)?;

            Ok(ScalarListElement {
                node_id,
                position,
                value,
            })
        })?;

        let mut list_values = vec![];
        for (node_id, elements) in &results.into_iter().group_by(|ele| ele.node_id.clone()) {
            let values = ScalarListValues {
                node_id,
                values: elements.into_iter().map(|e| e.value).collect(),
            };
            list_values.push(values);
        }

        Ok(list_values)
    }
}

// TODO: Check do we need the position at all.
#[allow(dead_code)]
struct ScalarListElement {
    node_id: GraphqlId,
    position: u32,
    value: PrismaValue,
}

impl SqlResolver<Sqlite> {
    fn read_row(row: &Row, selected_fields: &SelectedFields) -> PrismaResult<Node> {
        let mut fields = Vec::new();
        for (i, sf) in selected_fields.scalar_non_list().iter().enumerate() {
            fields.push(Self::fetch_value(sf.type_identifier, &row, i)?);
        }

        Ok(Node::new(fields))
    }

    fn fetch_int(row: &Row) -> i64 {
        row.get_checked(0).unwrap_or(0)
    }

    /// Converter function to wrap the limited set of types in SQLite to a
    /// richer PrismaValue.
    fn fetch_value(typ: TypeIdentifier, row: &Row, i: usize) -> PrismaResult<PrismaValue> {
        let result = match typ {
            TypeIdentifier::String => row.get_checked(i).map(|val| PrismaValue::String(val)),
            TypeIdentifier::GraphQLID => row.get_checked(i).map(|val| PrismaValue::GraphqlId(val)),
            TypeIdentifier::UUID => {
                let result: Result<String, rusqlite::Error> = row.get_checked(i);

                if let Ok(val) = result {
                    let uuid = Uuid::parse_str(val.as_ref())?;
                    Ok(PrismaValue::Uuid(uuid))
                }  else {
                    result.map(|s| PrismaValue::String(s))
                }
            },
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

        match result {
            Err(rusqlite::Error::InvalidColumnType(_, rusqlite::types::Type::Null)) => Ok(PrismaValue::Null),
            Ok(pv) => Ok(pv),
            Err(e) => Err(e.into())
        }
    }
}
