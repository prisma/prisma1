use super::*;
use prisma_query::ast::ParameterizedValue;
use prisma_query::connector::{Queryable, Sqlite};

pub struct IntrospectionConnector {
    pub queryable: Sqlite,
}

impl IntrospectionConnector {
    pub fn new(file_path: &str, db_name: &str) -> Result<IntrospectionConnector> {
        let mut queryable = Sqlite::new(file_path)?;
        queryable.attach_database(db_name)?;
        Ok(IntrospectionConnector { queryable })
    }

    fn get_table_names(&mut self, schema: &str) -> Vec<String> {
        let sql = format!("SELECT name FROM {}.sqlite_master WHERE type='table'", schema);

        let result_set = self.queryable.query_raw(&sql, &[]).unwrap();
        let names = result_set
            .into_iter()
            .map(|row| row.get("name").and_then(|x| x.to_string()).unwrap())
            .filter(|n| n != "sqlite_sequence")
            .collect();
        names
    }

    fn get_table(&mut self, schema: &str, table: &str) -> Table {
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

    fn get_columns(&mut self, schema: &str, table: &str) -> Vec<IntrospectedColumn> {
        let sql = format!(r#"Pragma "{}".table_info ("{}")"#, schema, table);

        let result_set = self.queryable.query_raw(&sql, &[]).unwrap();
        let columns = result_set
            .into_iter()
            .map(|row| {
                let default_value = match row.get("dflt_value") {
                    Some(ParameterizedValue::Text(v)) => Some(v.to_string()),
                    Some(ParameterizedValue::Null) => None,
                    Some(p) => panic!(format!("expected a string value but got {:?}", p)),
                    None => panic!("couldn't get dflt_value column"),
                };
                IntrospectedColumn {
                    name: row.get("name").and_then(|x| x.to_string()).unwrap(),
                    table: table.to_string(),
                    tpe: row.get("type").and_then(|x| x.to_string()).unwrap(),
                    is_required: row.get("notnull").and_then(|x| x.as_bool()).unwrap(),
                    default: default_value,
                    pk: row.get("pk").and_then(|x| x.as_i64()).unwrap() as u32,
                }
            })
            .collect();

        columns
    }

    fn get_foreign_constraints(&mut self, schema: &str, table: &str) -> Vec<IntrospectedForeignKey> {
        let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);

        let result_set = self.queryable.query_raw(&sql, &[]).unwrap();
        let foreign_keys = result_set
            .into_iter()
            .map(|row| IntrospectedForeignKey {
                name: "".to_string(),
                table: table.to_string(),
                column: row.get("from").and_then(|x| x.to_string()).unwrap(),
                referenced_table: row.get("table").and_then(|x| x.to_string()).unwrap(),
                referenced_column: row.get("to").and_then(|x| x.to_string()).unwrap(),
            })
            .collect();

        foreign_keys
    }
}

impl super::IntrospectionConnector for IntrospectionConnector {
    fn list_schemas(&self) -> Result<Vec<String>> {
        Ok(vec![])
    }

    fn introspect(&mut self, schema: &str) -> Result<DatabaseSchema> {
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
