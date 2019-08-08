use super::*;
use crate::IntrospectionConnection;
use log::debug;
use prisma_query::ast::ParameterizedValue;
use std::collections::HashMap;
use std::sync::Arc;

pub struct IntrospectionConnector {
    pub conn: Arc<IntrospectionConnection>,
}

impl super::IntrospectionConnector for IntrospectionConnector {
    fn list_schemas(&self) -> Result<Vec<String>> {
        Ok(vec![])
    }

    fn introspect(&self, schema: &str) -> Result<DatabaseSchema> {
        debug!("Introspecting schema '{}'", schema);
        let tables = self
            .get_table_names(schema)
            .into_iter()
            .map(|t| self.get_table(schema, &t))
            .collect();
        Ok(DatabaseSchema {
            // There's no enum type in SQLite.
            enums: vec![],
            // There are no sequences in SQLite.
            sequences: vec![],
            tables: tables,
        })
    }
}

impl IntrospectionConnector {
    pub fn new(conn: Arc<dyn IntrospectionConnection>) -> IntrospectionConnector {
        IntrospectionConnector { conn }
    }

    fn get_table_names(&self, schema: &str) -> Vec<String> {
        let sql = format!("SELECT name FROM {}.sqlite_master WHERE type='table'", schema);
        debug!("Introspecting table names with query: '{}'", sql);
        let result_set = self.conn.query_raw(&sql, schema).expect("get table names");
        let names = result_set
            .into_iter()
            .map(|row| row.get("name").and_then(|x| x.to_string()).unwrap())
            .filter(|n| n != "sqlite_sequence")
            .collect();
        debug!("Found table names: {:#?}", names);
        names
    }

    fn get_table(&self, schema: &str, name: &str) -> Table {
        debug!("Introspecting table '{}' in schema '{}", name, schema);
        let (introspected_columns, primary_key) = self.get_columns(schema, name);
        let foreign_keys = self.get_foreign_keys(schema, name);
        Table {
            name: name.to_string(),
            columns: introspected_columns,
            // TODO
            indices: Vec::new(),
            primary_key,
            foreign_keys,
        }
    }

    fn get_columns(&self, schema: &str, table: &str) -> (Vec<Column>, Option<PrimaryKey>) {
        let sql = format!(r#"Pragma "{}".table_info ("{}")"#, schema, table);
        debug!("Introspecting table columns, query: '{}'", sql);
        let result_set = self.conn.query_raw(&sql, schema).unwrap();
        let mut pk_cols: HashMap<i64, String> = HashMap::new();
        let mut cols: Vec<Column> = result_set
            .into_iter()
            .map(|row| {
                debug!("Got column row {:?}", row);
                let default_value = match row.get("dflt_value") {
                    Some(ParameterizedValue::Text(v)) => Some(v.to_string()),
                    Some(ParameterizedValue::Null) => None,
                    Some(p) => panic!(format!("expected a string value but got {:?}", p)),
                    None => panic!("couldn't get dflt_value column"),
                };
                let tpe = get_column_type(&row.get("type").and_then(|x| x.to_string()).expect("type"));
                let pk = row.get("pk").and_then(|x| x.as_i64()).expect("primary key");
                let is_required = row.get("notnull").and_then(|x| x.as_bool()).expect("notnull");
                let arity = if tpe.raw.ends_with("[]") {
                    ColumnArity::List
                } else if is_required {
                    ColumnArity::Required
                } else {
                    ColumnArity::Nullable
                };
                let col = Column {
                    name: row.get("name").and_then(|x| x.to_string()).expect("name"),
                    tpe,
                    arity: arity.clone(),
                    default: default_value.clone(),
                    // TODO
                    auto_increment: None,
                };
                if pk > 0 {
                    pk_cols.insert(pk, col.name.clone());
                }

                debug!(
                    "Found column '{}', type: '{:?}', default: '{}', arity: {:?}, primary key: {}",
                    col.name,
                    col.tpe,
                    default_value.unwrap_or("none".to_string()),
                    arity,
                    pk
                );

                col
            })
            .collect();
        cols.sort_unstable_by_key(|col| col.name.clone());

        let primary_key = match pk_cols.is_empty() {
            true => {
                debug!("Determined that table has no primary key");
                None
            }
            false => {
                let mut columns: Vec<String> = vec![];
                let mut col_idxs: Vec<&i64> = pk_cols.keys().collect();
                col_idxs.sort_unstable();
                for i in col_idxs {
                    columns.push(pk_cols[i].clone());
                }
                debug!("Determined that table has primary key with columns {:?}", columns);
                Some(PrimaryKey { columns })
            }
        };

        (cols, primary_key)
    }

    fn get_foreign_keys(&self, schema: &str, table: &str) -> Vec<ForeignKey> {
        struct IntermediateForeignKey {
            pub columns: HashMap<i64, String>,
            pub referenced_table: String,
            pub referenced_columns: HashMap<i64, String>,
        }

        let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);
        debug!("Introspecting table foreign keys, SQL: '{}'", sql);
        let result_set = self.conn.query_raw(&sql, schema).expect("querying for foreign keys");

        // Since one foreign key with multiple columns will be represented here as several
        // rows with the same ID, we have to use an intermediate representation that gets
        // translated into the real foreign keys in another pass
        let mut intermediate_fks: HashMap<i64, IntermediateForeignKey> = HashMap::new();
        for row in result_set.into_iter() {
            debug!("got FK introspection row {:?}", row);
            let id = row.get("id").and_then(|x| x.as_i64()).expect("id");
            let seq = row.get("seq").and_then(|x| x.as_i64()).expect("seq");
            let column = row.get("from").and_then(|x| x.to_string()).expect("from");
            let referenced_column = row.get("to").and_then(|x| x.to_string()).expect("to");
            let referenced_table = row.get("table").and_then(|x| x.to_string()).expect("table");
            match intermediate_fks.get_mut(&id) {
                Some(fk) => {
                    fk.columns.insert(seq, column);
                    fk.referenced_columns.insert(seq, referenced_column);
                }
                None => {
                    let mut columns: HashMap<i64, String> = HashMap::new();
                    columns.insert(seq, column);
                    let mut referenced_columns: HashMap<i64, String> = HashMap::new();
                    referenced_columns.insert(seq, referenced_column);
                    let fk = IntermediateForeignKey {
                        columns,
                        referenced_table,
                        referenced_columns,
                    };
                    intermediate_fks.insert(id, fk);
                }
            };
        }

        intermediate_fks
            .values()
            .into_iter()
            .map(|intermediate_fk| {
                let mut column_keys: Vec<&i64> = intermediate_fk.columns.keys().collect();
                column_keys.sort();
                let mut columns: Vec<String> = vec![];
                columns.reserve(column_keys.len());
                for i in column_keys {
                    columns.push(intermediate_fk.columns[i].to_owned());
                }

                let mut referenced_column_keys: Vec<&i64> = intermediate_fk.referenced_columns.keys().collect();
                referenced_column_keys.sort();
                let mut referenced_columns: Vec<String> = vec![];
                referenced_columns.reserve(referenced_column_keys.len());
                for i in referenced_column_keys {
                    referenced_columns.push(intermediate_fk.referenced_columns[i].to_owned());
                }

                let fk = ForeignKey {
                    columns,
                    referenced_table: intermediate_fk.referenced_table.to_owned(),
                    referenced_columns,
                    on_delete_action: ForeignKeyAction::NoAction,
                };
                debug!("Detected foreign key {:?}", fk);
                fk
            })
            .collect()
    }
}

#[derive(Debug)]
pub struct IntrospectedForeignKey {
    pub name: String,
    pub table: String,
    pub column: String,
    pub referenced_table: String,
    pub referenced_column: String,
}

#[derive(Debug, Clone)]
pub struct IntrospectedColumn {
    pub name: String,
    pub table: String,
    pub tpe: String,
    pub default: Option<String>,
    pub is_required: bool,
    pub pk: i64,
}

fn get_column_type(tpe: &str) -> ColumnType {
    let tpe_lower = tpe.to_lowercase();
    let family = match tpe_lower.as_ref() {
        // SQLite only has a few native data types: https://www.sqlite.org/datatype3.html
        // It's tolerant though, and you can assign any data type you like to columns
        "integer" => ColumnTypeFamily::Int,
        "real" => ColumnTypeFamily::Float,
        "float" => ColumnTypeFamily::Float,
        "serial" => ColumnTypeFamily::Int,
        "boolean" => ColumnTypeFamily::Boolean,
        "text" => ColumnTypeFamily::String,
        s if s.contains("char") => ColumnTypeFamily::String,
        "date" => ColumnTypeFamily::DateTime,
        "binary" => ColumnTypeFamily::Binary,
        "double" => ColumnTypeFamily::Float,
        "binary[]" => ColumnTypeFamily::Binary,
        "boolean[]" => ColumnTypeFamily::Boolean,
        "date[]" => ColumnTypeFamily::DateTime,
        "double[]" => ColumnTypeFamily::Float,
        "float[]" => ColumnTypeFamily::Float,
        "integer[]" => ColumnTypeFamily::Int,
        "text[]" => ColumnTypeFamily::String,
        x => panic!(format!("type '{}' is not supported here yet", x)),
    };
    ColumnType {
        raw: tpe.to_string(),
        family: family,
    }
}
