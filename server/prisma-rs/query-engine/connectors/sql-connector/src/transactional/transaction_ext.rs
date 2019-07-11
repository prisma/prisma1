use crate::{error::*, query_builder::QueryBuilder, AliasedCondition, RawQuery, SqlRow, ToSqlRow};
use connector_interface::{
    error::RecordFinderInfo,
    filter::{Filter, RecordFinder},
};
use prisma_models::*;
use prisma_query::{
    ast::*,
    connector::{Transaction, Queryable},
};
use serde_json::{Map, Number, Value};
use std::{convert::TryFrom, sync::Arc};

pub fn filter(conn: &mut Transaction, q: Query, idents: &[TypeIdentifier]) -> crate::Result<Vec<SqlRow>> {
    let result_set = conn.query(q)?;
    let mut sql_rows = Vec::new();

    for row in result_set {
        sql_rows.push(row.to_sql_row(idents)?);
    }

    Ok(sql_rows)
}

pub fn raw_json(conn: &mut Transaction, q: RawQuery) -> crate::Result<Value> {
    if q.is_select() {
        let result_set = conn.query_raw(q.0.as_str(), &[])?;
        let columns: Vec<String> = result_set.columns().map(ToString::to_string).collect();
        let mut result = Vec::new();

        for row in result_set.into_iter() {
            let mut object = Map::new();

            for (idx, p_value) in row.into_iter().enumerate() {
                let column_name: String = columns[idx].clone();
                object.insert(column_name, Value::from(p_value));
            }

            result.push(Value::Object(object));
        }

        Ok(Value::Array(result))
    } else {
        let changes = conn.execute_raw(q.0.as_str(), &[])?;
        Ok(Value::Number(Number::from(changes)))
    }
}

/// Find one full record selecting all scalar fields.
pub fn find_record(conn: &mut Transaction, record_finder: &RecordFinder) -> crate::Result<SingleRecord> {
    use SqlError::*;

    let model = record_finder.field.model();
    let selected_fields = SelectedFields::from(Arc::clone(&model));
    let select = QueryBuilder::get_records(model, &selected_fields, record_finder);
    let idents = selected_fields.type_identifiers();

    let row = find(conn, select, idents.as_slice()).map_err(|e| match e {
        RecordDoesNotExist => RecordNotFoundForWhere(RecordFinderInfo::from(record_finder)),
        e => e,
    })?;

    let record = Record::from(row);

    Ok(SingleRecord::new(record, selected_fields.names()))
}

/// Select one row from the database.
pub fn find(conn: &mut Transaction, q: Select, idents: &[TypeIdentifier]) -> crate::Result<SqlRow> {
    filter(conn, q.limit(1).into(), idents)?
        .into_iter()
        .next()
        .ok_or(SqlError::RecordDoesNotExist)
}

/// Read the first column from the first row as an integer.
pub fn find_int(conn: &mut Transaction, q: Select) -> crate::Result<i64> {
    // UNWRAP: A dataset will always have at least one column, even if it contains no data.
    let id = find(conn, q, &[TypeIdentifier::Int])?.values.into_iter().next().unwrap();

    Ok(i64::try_from(id)?)
}

/// Read the first column from the first row as an `GraphqlId`.
pub fn find_id(conn: &mut Transaction, record_finder: &RecordFinder) -> crate::Result<GraphqlId> {
    let model = record_finder.field.model();
    let filter = Filter::from(record_finder.clone());

    let id = filter_ids(conn, model, filter)?
        .into_iter()
        .next()
        .ok_or_else(|| SqlError::RecordNotFoundForWhere(RecordFinderInfo::from(record_finder)))?;

    Ok(id)
}

/// Read the all columns as an `GraphqlId`
pub fn filter_ids(conn: &mut Transaction, model: ModelRef, filter: Filter) -> crate::Result<Vec<GraphqlId>> {
    let select = Select::from_table(model.table())
        .column(model.fields().id().as_column())
        .so_that(filter.aliased_cond(None));

    select_ids(conn, select)
}

pub fn select_ids(conn: &mut Transaction, select: Select) -> crate::Result<Vec<GraphqlId>> {
    let mut rows = filter(conn, select.into(), &[TypeIdentifier::GraphQLID])?;
    let mut result = Vec::new();

    for mut row in rows.drain(0..) {
        for value in row.values.drain(0..) {
            result.push(GraphqlId::try_from(value)?)
        }
    }

    Ok(result)
}

/// Find a child of a parent. Will return an error if no child found with
/// the given parameters. A more restrictive version of `get_ids_by_parents`.
pub fn find_id_by_parent(
    conn: &mut Transaction,
    parent_field: RelationFieldRef,
    parent_id: &GraphqlId,
    selector: &Option<RecordFinder>,
) -> crate::Result<GraphqlId> {
    let ids = filter_ids_by_parents(
        conn,
        Arc::clone(&parent_field),
        vec![parent_id],
        selector.clone().map(Filter::from),
    )?;

    let id = ids.into_iter().next().ok_or_else(|| SqlError::RecordsNotConnected {
        relation_name: parent_field.relation().name.clone(),
        parent_name: parent_field.model().name.clone(),
        parent_where: None,
        child_name: parent_field.related_model().name.clone(),
        child_where: selector.as_ref().map(RecordFinderInfo::from),
    })?;

    Ok(id)
}

/// Find all children record id's with the given parent id's, optionally given
/// a `Filter` for extra filtering.
pub fn filter_ids_by_parents(
    conn: &mut Transaction,
    parent_field: RelationFieldRef,
    parent_ids: Vec<&GraphqlId>,
    selector: Option<Filter>,
) -> crate::Result<Vec<GraphqlId>> {
    let related_model = parent_field.related_model();
    let relation = parent_field.relation();
    let child_id_field = relation.column_for_relation_side(parent_field.relation_side.opposite());
    let parent_id_field = relation.column_for_relation_side(parent_field.relation_side);

    let subselect = Select::from_table(relation.relation_table())
        .column(child_id_field)
        .so_that(parent_id_field.in_selection(parent_ids));

    let conditions = related_model
        .fields()
        .id()
        .db_name()
        .to_string()
        .in_selection(subselect);

    let conditions = match selector {
        Some(into_cond) => {
            let filter: Filter = into_cond.into();
            conditions.and(filter.aliased_cond(None))
        }
        None => conditions.into(),
    };

    let select = Select::from_table(related_model.table())
        .column(related_model.fields().id().as_column())
        .so_that(conditions);

    select_ids(conn, select)
}
