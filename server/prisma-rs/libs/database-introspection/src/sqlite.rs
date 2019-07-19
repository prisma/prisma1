use super::*;
use prisma_query::connector::Sqlite as SqliteDriver;
use prisma_query::{ast::ParameterizedValue, Connectional};
use std::sync::Arc;

pub struct IntrospectionConnector {
    pub connectional: Arc<Connectional>,
}

impl IntrospectionConnector {
    pub fn new(file_path: &str) -> Result<IntrospectionConnector> {
        let connectional = SqliteDriver::new(String::from(file_path), 1, false)?;
        Ok(IntrospectionConnector {
            connectional: Arc::new(connectional),
        })
    }

    fn get_table_names(&self, schema: &str) -> Vec<String> {
        let sql = format!("SELECT name FROM {}.sqlite_master WHERE type='table'", schema);

        let result_set = self.connectional.query_on_raw_connection(&schema, &sql, &[]).unwrap();
        let names = result_set
            .into_iter()
            .map(|row| row.get_as_string("name").unwrap())
            .filter(|n| n != "sqlite_sequence")
            .collect();
        names
    }

    fn get_table(&self, schema: &str, table: &str) -> Table {
        let introspected_columns = self.get_columns(&schema, &table);
        let introspected_foreign_keys = self.get_foreign_constraints(&schema, &table);

        let mut columns_copy = introspected_columns.clone();
        columns_copy.sort_by_key(|c| c.pk);
        Table {
            name: table.to_string(),
            columns: convert_introspected_columns(
                introspected_columns,
                introspected_foreign_keys,
                Box::new(column_type),
            ),
            indexes: Vec::new(),
            primary_key: None,
            foreign_keys: vec![],
        }
    }

    fn get_columns(&self, schema: &str, table: &str) -> Vec<IntrospectedColumn> {
        let sql = format!(r#"Pragma "{}".table_info ("{}")"#, schema, table);

        let result_set = self.connectional.query_on_raw_connection(&schema, &sql, &[]).unwrap();
        let columns = result_set
            .into_iter()
            .map(|row| {
                let default_value = match row.get("dflt_value") {
                    Ok(ParameterizedValue::Text(v)) => Some(v.to_string()),
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

        columns
    }

    fn get_foreign_constraints(&self, schema: &str, table: &str) -> Vec<IntrospectedForeignKey> {
        let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);

        let result_set = self.connectional.query_on_raw_connection(&schema, &sql, &[]).unwrap();

        let foreign_keys = result_set
            .into_iter()
            .map(|row| IntrospectedForeignKey {
                name: "".to_string(),
                table: table.to_string(),
                column: row.get_as_string("from").unwrap(),
                referenced_table: row.get_as_string("table").unwrap(),
                referenced_column: row.get_as_string("to").unwrap(),
            })
            .collect();

        foreign_keys
    }
}

impl super::IntrospectionConnector for IntrospectionConnector {
    fn list_schemas(&self) -> Result<Vec<String>> {
        Ok(vec![])
    }

    fn introspect(&self, schema: &str) -> Result<DatabaseSchema> {
        let tables = self
            .get_table_names(schema)
            .into_iter()
            .map(|t| self.get_table(schema, &t))
            .collect();
        Ok(DatabaseSchema {
            enums: vec![],
            sequences: vec![],
            tables: tables,
        })
    }
}

fn convert_introspected_columns(
    columns: Vec<IntrospectedColumn>,
    foreign_keys: Vec<IntrospectedForeignKey>,
    column_type: Box<Fn(&IntrospectedColumn) -> ColumnType>,
) -> Vec<Column> {
    columns
        .iter()
        .map(|c| {
            let arity = match c.is_required {
                true => ColumnArity::Required,
                false => ColumnArity::Nullable,
            };
            Column {
                name: c.name.clone(),
                tpe: column_type(c),
                arity: arity,
                default: None,
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
    pub pk: u32,
}

fn column_type(column: &IntrospectedColumn) -> ColumnType {
    let tpe = column.tpe.to_lowercase();
    let family = match tpe.as_ref() {
        "integer" => ColumnTypeFamily::Int,
        "real" => ColumnTypeFamily::Float,
        "boolean" => ColumnTypeFamily::Boolean,
        "text" => ColumnTypeFamily::String,
        s if s.contains("char") => ColumnTypeFamily::String,
        "date" => ColumnTypeFamily::DateTime,
        "binary" => ColumnTypeFamily::Binary,
        "double" => ColumnTypeFamily::Double,
        "binary[]" => ColumnTypeFamily::BinArray,
        "boolean[]" => ColumnTypeFamily::BoolArray,
        "date[]" => ColumnTypeFamily::DateTimeArray,
        "double[]" => ColumnTypeFamily::DoubleArray,
        "float[]" => ColumnTypeFamily::FloatArray,
        "integer[]" => ColumnTypeFamily::IntArray,
        "text[]" => ColumnTypeFamily::StringArray,
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
