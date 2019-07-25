use super::*;
use log::debug;
use prisma_query::ast::ParameterizedValue;
use prisma_query::connector::{Queryable, Sqlite};
use std::collections::HashMap;

pub struct IntrospectionConnector {
    pub queryable: Sqlite,
}

impl IntrospectionConnector {
    pub fn new(file_path: &str, db_name: &str) -> Result<IntrospectionConnector> {
        debug!(
            "Creating SQLite IntrospectionConnector, opening '{}' as '{}",
            file_path, db_name
        );
        let mut queryable = Sqlite::new(file_path)?;
        queryable.attach_database(db_name)?;
        Ok(IntrospectionConnector { queryable })
    }

    fn get_table_names(&mut self, schema: &str) -> Vec<String> {
        let sql = format!("SELECT name FROM {}.sqlite_master WHERE type='table'", schema);
        debug!("Introspecting table names with query: '{}'", sql);
        let result_set = self.queryable.query_raw(&sql, &[]).unwrap();
        let names = result_set
            .into_iter()
            .map(|row| row.get("name").and_then(|x| x.to_string()).unwrap())
            .filter(|n| n != "sqlite_sequence")
            .collect();
        debug!("Found table names: {:#?}", names);
        names
    }

    fn get_table(&mut self, schema: &str, name: &str) -> Table {
        debug!("Introspecting table '{}' in schema '{}", name, schema);
        let (introspected_columns, primary_key) = self.get_columns(&schema, name);
        let foreign_keys = self.get_foreign_keys(&schema, name);
        Table {
            name: name.to_string(),
            columns: convert_introspected_columns(introspected_columns),
            // TODO
            indexes: Vec::new(),
            primary_key,
            // TODO
            foreign_keys,
        }
    }

    fn get_columns(&mut self, schema: &str, table: &str) -> (Vec<IntrospectedColumn>, Option<PrimaryKey>) {
        let sql = format!(r#"Pragma "{}".table_info ("{}")"#, schema, table);
        debug!("Introspecting table columns, query: '{}'", sql);
        let result_set = self.queryable.query_raw(&sql, &[]).unwrap();
        let mut pk_cols: HashMap<i64, String> = HashMap::new();
        let cols = result_set
            .into_iter()
            .map(|row| {
                let default_value = match row.get("dflt_value") {
                    Some(ParameterizedValue::Text(v)) => Some(v.to_string()),
                    Some(ParameterizedValue::Null) => None,
                    Some(p) => panic!(format!("expected a string value but got {:?}", p)),
                    None => panic!("couldn't get dflt_value column"),
                };
                let col = IntrospectedColumn {
                    name: row.get("name").and_then(|x| x.to_string()).expect("name"),
                    table: table.to_string(),
                    tpe: row.get("type").and_then(|x| x.to_string()).expect("type"),
                    is_required: row.get("notnull").and_then(|x| x.as_bool()).expect("notnull"),
                    default: default_value.clone(),
                    pk: row.get("pk").and_then(|x| x.as_i64()).expect("primary key"),
                };
                if col.pk > 0 {
                    pk_cols.insert(col.pk, col.name.clone());
                }

                debug!(
                    "Found column '{}', type: '{}', is_required: {}, default: '{}', primary key: {}",
                    col.name,
                    col.tpe,
                    col.is_required,
                    default_value.unwrap_or("none".to_string()),
                    col.pk
                );

                col
            })
            .collect();

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

    fn get_foreign_keys(&mut self, schema: &str, table: &str) -> Vec<ForeignKey> {
        let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);
        debug!("Introspecting table foreign keys, SQL: '{}'", sql);
        let result_set = self.queryable.query_raw(&sql, &[]).expect("querying for foreign keys");
        result_set
            .into_iter()
            .map(|row| {
                let fk = ForeignKey {
                    column: row.get("from").and_then(|x| x.to_string()).expect("from"),
                    referenced_table: row.get("table").and_then(|x| x.to_string()).expect("table"),
                    referenced_column: row.get("to").and_then(|x| x.to_string()).expect("to"),
                };
                debug!(
                    "Found foreign key column: '{}', to table: '{}', to column: '{}'",
                    fk.column, fk.referenced_table, fk.referenced_column
                );
                fk
            })
            .collect()
    }
}

impl super::IntrospectionConnector for IntrospectionConnector {
    fn list_schemas(&self) -> Result<Vec<String>> {
        Ok(vec![])
    }

    fn introspect(&mut self, schema: &str) -> Result<DatabaseSchema> {
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

fn convert_introspected_columns(columns: Vec<IntrospectedColumn>) -> Vec<Column> {
    columns
        .iter()
        .map(|c| {
            let arity = match c.is_required {
                true => ColumnArity::Required,
                false => ColumnArity::Nullable,
            };
            Column {
                name: c.name.clone(),
                tpe: get_column_type(c),
                arity: arity,
                default: None,
                // TODO
                auto_increment: None,
            }
        })
        .collect()
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

fn get_column_type(column: &IntrospectedColumn) -> ColumnType {
    let tpe = column.tpe.to_lowercase();
    let family = match tpe.as_ref() {
        "integer" => ColumnTypeFamily::Int,
        "real" => ColumnTypeFamily::Float,
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
        x => panic!(format!(
            "type '{}' is not supported here yet. Column was: {}",
            x, column.name
        )),
    };
    ColumnType {
        raw: column.tpe.clone(),
        family: family,
    }
}
