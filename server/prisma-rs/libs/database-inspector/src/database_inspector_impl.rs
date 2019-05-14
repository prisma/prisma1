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
                .map(|t| Table {
                    name: t,
                    columns: Vec::new(),
                    indexes: Vec::new(),
                })
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
            result.push(name);
        }

        result
    }
}

fn get_table(schema: &String, table: &String) -> Table {
    let _cols = get_column(&schema, &table);
    let _foreign = get_foreign_constraint(&schema, &table);
    let _index = get_index(&schema, &table);
    let _seq = get_sequence(&schema, &table);

    unimplemented!()
}

fn get_column(_schema: &String, _table: &String) -> Column {
    unimplemented!()
}

fn get_foreign_constraint(_schema: &String, _table: &String) -> ForeignKey {
    unimplemented!()
}

fn get_sequence(_schema: &String, _table: &String) -> Sequence {
    unimplemented!()
}

fn get_index(_schema: &String, _table: &String) -> Index {
    unimplemented!()
}
