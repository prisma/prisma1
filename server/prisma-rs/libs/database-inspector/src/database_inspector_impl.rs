use crate::*;

use rusqlite::{Connection, Result, NO_PARAMS};

pub struct DatabaseInspectorImpl {
    connection: Connection,
}

impl DatabaseInspector for DatabaseInspectorImpl {
    fn introspect(&self, schema: String) -> DatabaseSchema {
        DatabaseSchema {
            tables: self
                .get_table_names(&schema)
                .into_iter()
                .map(|t| self.get_table(&schema, &t))
                .collect(),
        }
    }
}

impl DatabaseInspectorImpl {
    pub fn new(connection: Connection) -> DatabaseInspectorImpl {
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

        let mut stmt = self.connection.prepare_cached(&sql).unwrap();
        let mut rows = stmt.query(NO_PARAMS).unwrap();
        let mut result = Vec::new();

        while let Some(row) = rows.next() {
            let name: String = row.unwrap().get("name");
            if name != "sqlite_sequence" {
                result.push(name);
            }
        }

        result
    }

    fn get_table(&self, schema: &String, table: &String) -> Table {
        let introspected_columns = self.get_columns(&schema, &table);
        let introspected_foreign_keys = self.get_foreign_constraints(&schema, &table);
        // let _index = get_index(&schema, &table);
        // let _seq = get_sequence(&schema, &table);

        Table {
            name: table.to_string(),
            columns: convert_introspected_columns(introspected_columns, introspected_foreign_keys),
            indexes: Vec::new(),
        }
    }

    fn get_columns(&self, schema: &String, table: &String) -> Vec<IntrospectedColumn> {
        let sql = format!(r#"Pragma "{}".table_info ("{}")"#, schema, table);
        let mut stmt = self.connection.prepare_cached(&sql).unwrap();
        let mut rows = stmt.query(NO_PARAMS).unwrap();
        let mut result = Vec::new();

        while let Some(row_result) = rows.next() {
            let row = row_result.unwrap();
            result.push(IntrospectedColumn {
                name: row.get("name"),
                table: table.to_string(),
                tpe: row.get("type"),
                is_required: row.get("notnull"),
                default: row.get("dflt_value"),
            });
        }

        result
    }

    fn get_foreign_constraints(&self, schema: &String, table: &String) -> Vec<IntrospectedForeignKey> {
        let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);
        let mut stmt = self.connection.prepare_cached(&sql).unwrap();
        let mut rows = stmt.query(NO_PARAMS).unwrap();
        let mut result = Vec::new();

        while let Some(row_result) = rows.next() {
            let row = row_result.unwrap();
            result.push(IntrospectedForeignKey {
                name: "".to_string(),
                table: table.to_string(),
                column: row.get("from"),
                referenced_table: row.get("table"),
                referenced_column: row.get("to"),
            });
        }

        result
    }

    fn get_sequence(&self, _schema: &String, _table: &String) -> Sequence {
        unimplemented!()
    }

    fn get_index(&self, _schema: &String, _table: &String) -> Index {
        unimplemented!()
    }

    // fn query<F>(&self, schema: &String, parse: F) ->
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

#[derive(Debug)]
struct IntrospectedColumn {
    name: String,
    table: String,
    tpe: String,
    default: Option<String>,
    is_required: bool,
}

#[derive(Debug)]
struct IntrospectedForeignKey {
    name: String,
    table: String,
    column: String,
    referenced_table: String,
    referenced_column: String,
}
