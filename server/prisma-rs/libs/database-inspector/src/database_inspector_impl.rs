use crate::*;

use prisma_query::ast::ParameterizedValue;
use prisma_query::{Connectional, ResultSet};
use rusqlite::{Connection, NO_PARAMS};
use std::sync::Arc;

pub struct DatabaseInspectorImpl<C: Connectional> {
    pub connection: Arc<C>,
}

impl<C: Connectional> DatabaseInspector for DatabaseInspectorImpl<C> {
    fn introspect(&self, schema: &String) -> DatabaseSchema {
        DatabaseSchema {
            tables: self
                .get_table_names(schema)
                .into_iter()
                .map(|t| self.get_table(schema, &t))
                .collect(),
        }
    }
}

impl<C: Connectional> DatabaseInspectorImpl<C> {
    pub fn new<Conn: Connectional>(connection: Arc<Conn>) -> DatabaseInspectorImpl<Conn> {
        DatabaseInspectorImpl { connection }
    }

    fn get_table_names(&self, schema: &String) -> Vec<String> {
        let sql = format!(
            "
            SELECT
                name
            FROM
                {}.sqlite_master
            WHERE
                type='table'
        ",
            schema
        );

        // let mut stmt = self.connection.prepare_cached(&sql).unwrap();
        // let mut rows = stmt.query(NO_PARAMS).unwrap();
        // let mut result = Vec::new();

        // while let Some(row) = rows.next().unwrap() {
        //     let name: String = row.get_unwrap("name");
        //     if name != "sqlite_sequence" {
        //         result.push(name);
        //     }
        // }
        // result

        self.connection
            .with_connection(&schema, |conn| {
                let result_set = conn.query_raw(&sql, &[]).unwrap();

                let names = result_set
                    .into_iter()
                    .map(|row| row.get_as_string("name").unwrap())
                    .filter(|n| n != "sqlite_sequence")
                    .collect();
                Ok(names)
            })
            .unwrap()
    }

    fn get_table(&self, schema: &String, table: &String) -> Table {
        let introspected_columns = self.get_columns(&schema, &table);
        let introspected_foreign_keys = self.get_foreign_constraints(&schema, &table);

        let mut columns_copy = introspected_columns.clone();
        columns_copy.sort_by_key(|c| c.pk);
        let pk_columns = columns_copy
            .into_iter()
            .filter(|c| c.pk > 0) // only the columns with a value greater 0 are part of the primary key
            .map(|c| c.name)
            .collect();

        Table {
            name: table.to_string(),
            columns: convert_introspected_columns(introspected_columns, introspected_foreign_keys),
            indexes: Vec::new(),
            primary_key_columns: pk_columns,
        }
    }

    fn get_columns(&self, schema: &String, table: &String) -> Vec<IntrospectedColumn> {
        let sql = format!(r#"Pragma "{}".table_info ("{}")"#, schema, table);
        // let mut stmt = self.connection.prepare_cached(&sql).unwrap();
        // let mut rows = stmt.query(NO_PARAMS).unwrap();
        // let mut result = Vec::new();

        // while let Some(row) = rows.next().unwrap() {
        //     result.push(IntrospectedColumn {
        //         name: row.get_unwrap("name"),
        //         table: table.to_string(),
        //         tpe: row.get_unwrap("type"),
        //         is_required: row.get_unwrap("notnull"),
        //         default: row.get_unwrap("dflt_value"),
        //         pk: row.get_unwrap("pk"),
        //     });
        // }
        // result

        self.connection
            .with_connection(&schema, |conn| {
                let result_set = conn.query_raw(&sql, &[]).unwrap();

                let names = result_set
                    .into_iter()
                    .map(|row| {
                        let default_value = match row.get("dflt_value") {
                            Ok(ParameterizedValue::Text(v)) => Some(v.clone()),
                            Ok(ParameterizedValue::Null) => None,
                            Ok(p) => panic!(format!("expectd a string value but got {:?}", p)),
                            Err(err) => panic!(format!("{}", err)),
                        };
                        IntrospectedColumn {
                            name: row.get_as_string("name").unwrap(),
                            table: table.to_string(),
                            tpe: row.get_as_string("type").unwrap(),
                            is_required: row.get_as_bool("notnull").unwrap(),
                            default: default_value,
                            pk: row.get_as_integer("pk").unwrap() as u32,
                        }
                    })
                    .collect();
                Ok(names)
            })
            .unwrap()
    }

    fn get_foreign_constraints(&self, schema: &String, table: &String) -> Vec<IntrospectedForeignKey> {
        let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);
        // let mut stmt = self.connection.prepare_cached(&sql).unwrap();
        // let mut rows = stmt.query(NO_PARAMS).unwrap();
        // let mut result = Vec::new();

        // while let Some(row) = rows.next().unwrap() {
        //     result.push(IntrospectedForeignKey {
        //         name: "".to_string(),
        //         table: table.to_string(),
        //         column: row.get_unwrap("from"),
        //         referenced_table: row.get_unwrap("table"),
        //         referenced_column: row.get_unwrap("to"),
        //     });
        // }

        // result

        self.connection
            .with_connection(&schema, |conn| {
                let result_set= conn.query_raw(&sql, &[]).unwrap();

                let names = result_set
                    .into_iter()
                    .map(|row| IntrospectedForeignKey {
                        name: "".to_string(),
                        table: table.to_string(),
                        column: row.get_as_string("from").unwrap(),
                        referenced_table: row.get_as_string("table").unwrap(),
                        referenced_column: row.get_as_string("to").unwrap(),
                    })
                    .collect();
                Ok(names)
            })
            .unwrap()
    }

    #[allow(unused)]
    fn get_sequence(&self, _schema: &String, _table: &String) -> Sequence {
        unimplemented!()
    }

    #[allow(unused)]
    fn get_index(&self, _schema: &String, _table: &String) -> Index {
        unimplemented!()
    }
}

fn convert_introspected_columns(
    columns: Vec<IntrospectedColumn>,
    foreign_keys: Vec<IntrospectedForeignKey>,
) -> Vec<Column> {
    columns
        .iter()
        .map(|c| {
            let foreign_key = foreign_keys
                .iter()
                .find(|fk| fk.column == c.name && fk.table == c.table)
                .map(|fk| ForeignKey {
                    table: fk.referenced_table.clone(),
                    column: fk.referenced_column.clone(),
                });
            Column {
                name: c.name.clone(),
                tpe: column_type(c),
                is_required: c.is_required,
                foreign_key: foreign_key,
                sequence: None,
            }
        })
        .collect()
}

fn column_type(column: &IntrospectedColumn) -> ColumnType {
    match column.tpe.as_ref() {
        "INTEGER" => ColumnType::Int,
        "REAL" => ColumnType::Float,
        "BOOLEAN" => ColumnType::Boolean,
        "TEXT" => ColumnType::String,
        s if s.starts_with("VARCHAR") => ColumnType::String,
        "DATE" => ColumnType::DateTime,
        x => panic!(format!(
            "type {} is not supported here yet. Column was: {}",
            x, column.name
        )),
    }
}

#[derive(Debug, Clone)]
struct IntrospectedColumn {
    name: String,
    table: String,
    tpe: String,
    default: Option<String>,
    is_required: bool,
    pk: u32,
}

#[derive(Debug)]
struct IntrospectedForeignKey {
    name: String,
    table: String,
    column: String,
    referenced_table: String,
    referenced_column: String,
}
