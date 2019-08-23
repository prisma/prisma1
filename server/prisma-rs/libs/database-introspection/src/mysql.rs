//! MySQL introspection.
use super::*;
use log::debug;
use std::collections::HashMap;
use std::sync::Arc;

/// IntrospectionConnector implementation.
pub struct IntrospectionConnector {
    conn: Arc<dyn IntrospectionConnection>,
}

impl super::IntrospectionConnector for IntrospectionConnector {
    fn list_schemas(&self) -> IntrospectionResult<Vec<String>> {
        Ok(vec![])
    }

    fn introspect(&self, schema: &str) -> IntrospectionResult<DatabaseSchema> {
        debug!("Introspecting schema '{}'", schema);
        let tables = self
            .get_table_names(schema)
            .into_iter()
            .map(|t| self.get_table(schema, &t))
            .collect();
        Ok(DatabaseSchema {
            tables,
            enums: vec![],
            sequences: vec![],
        })
    }
}

impl IntrospectionConnector {
    /// Constructor.
    pub fn new(conn: Arc<dyn IntrospectionConnection>) -> IntrospectionConnector {
        IntrospectionConnector { conn }
    }

    fn get_table_names(&self, schema: &str) -> Vec<String> {
        debug!("Getting table names");
        let sql = format!(
            "SELECT table_name as table_name FROM information_schema.tables
            WHERE table_schema = '{}'
            -- Views are not supported yet
            AND table_type = 'BASE TABLE'
            ORDER BY table_name",
            schema
        );
        let rows = self.conn.query_raw(&sql, schema).expect("get table names ");
        let names = rows
            .into_iter()
            .map(|row| {
                row.get("table_name")
                    .and_then(|x| x.to_string())
                    .expect("get table name")
            })
            .collect();

        debug!("Found table names: {:?}", names);
        names
    }

    fn get_table(&self, schema: &str, name: &str) -> Table {
        debug!("Getting table '{}'", name);
        let columns = self.get_columns(schema, name);
        let foreign_keys = self.get_foreign_keys(schema, name);
        let fk_cols = foreign_keys
            .iter()
            .flat_map(|fk| fk.columns.iter().map(|col| col.clone()))
            .collect();
        let (indices, primary_key) = self.get_indices(schema, name, fk_cols);
        Table {
            name: name.to_string(),
            columns,
            foreign_keys,
            indices,
            primary_key,
        }
    }

    fn get_columns(&self, schema: &str, table: &str) -> Vec<Column> {
        let sql = format!(
            "SELECT column_name, data_type, column_default, is_nullable, extra
            FROM information_schema.columns
            WHERE table_schema = '{}' AND table_name  = '{}'
            ORDER BY column_name",
            schema, table
        );
        let rows = self.conn.query_raw(&sql, schema).expect("querying for columns");
        let cols = rows
            .into_iter()
            .map(|col| {
                debug!("Got column: {:?}", col);
                let data_type = col.get("data_type").and_then(|x| x.to_string()).expect("get data_type");
                let is_nullable = col
                    .get("is_nullable")
                    .and_then(|x| x.to_string())
                    .expect("get is_nullable")
                    .to_lowercase();
                let is_required = match is_nullable.as_ref() {
                    "no" => true,
                    "yes" => false,
                    x => panic!(format!("unrecognized is_nullable variant '{}'", x)),
                };
                let tpe = get_column_type(data_type.as_ref());
                let arity = if tpe.raw.starts_with("_") {
                    ColumnArity::List
                } else if is_required {
                    ColumnArity::Required
                } else {
                    ColumnArity::Nullable
                };
                let extra = col
                    .get("extra")
                    .and_then(|x| x.to_string())
                    .expect("get extra")
                    .to_lowercase();
                let auto_increment = match extra.as_str() {
                    "auto_increment" => true,
                    _ => false,
                };
                Column {
                    name: col
                        .get("column_name")
                        .and_then(|x| x.to_string())
                        .expect("get column name"),
                    tpe,
                    arity,
                    default: col
                        .get("column_default")
                        .map(|x| {
                            debug!("Converting default to string: {:?}", x);
                            if x.is_null() {
                                None
                            } else {
                                let default = x.to_string().expect("default to string");
                                Some(default)
                            }
                        })
                        .expect("get default"),
                    auto_increment: auto_increment,
                }
            })
            .collect();

        debug!("Found table columns: {:?}", cols);
        cols
    }

    fn get_foreign_keys(&self, schema: &str, table: &str) -> Vec<ForeignKey> {
        // XXX: Is constraint_name unique? Need a way to uniquely associate rows with foreign keys
        // One should think it's unique since it's used to join information_schema.key_column_usage
        // and information_schema.referential_constraints tables in this query lifted from
        // Stack Overflow
        let sql = format!(
            "SELECT kcu.constraint_name, kcu.column_name, kcu.referenced_table_name, 
            kcu.referenced_column_name, kcu.ordinal_position, rc.delete_rule
            FROM information_schema.key_column_usage AS kcu
            INNER JOIN information_schema.referential_constraints AS rc ON
            kcu.constraint_name = rc.constraint_name
            WHERE kcu.table_schema = '{}' AND kcu.table_name = '{}' AND 
            referenced_column_name IS NOT NULL
        ",
            schema, table
        );
        debug!("Introspecting table foreign keys, SQL: '{}'", sql);

        let result_set = self.conn.query_raw(&sql, schema).expect("querying for foreign keys");
        let mut intermediate_fks: HashMap<String, ForeignKey> = HashMap::new();
        for row in result_set.into_iter() {
            debug!("Got introspection FK row {:#?}", row);
            let constraint_name = row
                .get("constraint_name")
                .and_then(|x| x.to_string())
                .expect("get constraint_name");
            let column = row
                .get("column_name")
                .and_then(|x| x.to_string())
                .expect("get column_name");
            let referenced_table = row
                .get("referenced_table_name")
                .and_then(|x| x.to_string())
                .expect("get referenced_table_name");
            let referenced_column = row
                .get("referenced_column_name")
                .and_then(|x| x.to_string())
                .expect("get referenced_column_name");
            let ord_pos = row
                .get("ordinal_position")
                .and_then(|x| x.as_i64())
                .expect("get ordinal_position");
            let on_delete_action = match row
                .get("delete_rule")
                .and_then(|x| x.to_string())
                .expect("get delete_rule")
                .to_lowercase()
                .as_str()
            {
                "cascade" => ForeignKeyAction::Cascade,
                "set null" => ForeignKeyAction::SetNull,
                "set default" => ForeignKeyAction::SetDefault,
                "restrict" => ForeignKeyAction::Restrict,
                "no action" => ForeignKeyAction::NoAction,
                s @ _ => panic!(format!("Unrecognized on delete action '{}'", s)),
            };
            match intermediate_fks.get_mut(&constraint_name) {
                Some(fk) => {
                    let pos = ord_pos as usize - 1;
                    if fk.columns.len() <= pos {
                        fk.columns.resize(pos + 1, "".to_string());
                    }
                    fk.columns[pos] = column;
                    if fk.referenced_columns.len() <= pos {
                        fk.referenced_columns.resize(pos + 1, "".to_string());
                    }
                    fk.referenced_columns[pos] = referenced_column;
                }
                None => {
                    let fk = ForeignKey {
                        columns: vec![column],
                        referenced_table,
                        referenced_columns: vec![referenced_column],
                        on_delete_action,
                    };
                    intermediate_fks.insert(constraint_name, fk);
                }
            };
        }

        let fks: Vec<ForeignKey> = intermediate_fks
            .values()
            .map(|intermediate_fk| intermediate_fk.to_owned())
            .collect();
        for fk in fks.iter() {
            debug!(
                "Found foreign key - column(s): {:?}, to table: '{}', to column(s): {:?}",
                fk.columns, fk.referenced_table, fk.referenced_columns
            );
        }

        fks
    }

    fn get_indices(&self, schema: &str, table_name: &str, fk_cols: Vec<String>) -> (Vec<Index>, Option<PrimaryKey>) {
        let sql = format!(
            "SELECT DISTINCT
                index_name, non_unique, column_name, seq_in_index
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE table_schema = '{}' AND table_name = '{}'
        ",
            schema, table_name
        );
        debug!("Introspecting indices, SQL: {}", sql);
        let rows = self.conn.query_raw(&sql, schema).expect("querying for indices");
        let mut pk: Option<PrimaryKey> = None;
        let indices = rows
            .into_iter()
            .filter_map(|index| {
                debug!("Got index row: {:#?}", index);
                let index_name = index.get("index_name").and_then(|x| x.to_string()).expect("index_name");
                let is_pk = index_name == "PRIMARY";
                let column_name = index
                    .get("column_name")
                    .and_then(|x| x.to_string())
                    .expect("column_name");
                let is_unique = !index.get("non_unique").and_then(|x| x.as_bool()).expect("non_unique");
                if is_pk {
                    pk = Some(PrimaryKey {
                        columns: vec![column_name],
                    });
                    None
                } else if fk_cols.contains(&column_name) {
                    None
                } else {
                    Some(Index {
                        name: index_name,
                        columns: vec![column_name],
                        unique: is_unique,
                    })
                }
            })
            .collect();

        debug!("Found table indices: {:?}, primary key: {:?}", indices, pk);
        (indices, pk)
    }
}

fn get_column_type(data_type: &str) -> ColumnType {
    let family = match data_type {
        "int" => ColumnTypeFamily::Int,
        "smallint" => ColumnTypeFamily::Int,
        "tinyint" => ColumnTypeFamily::Int,
        "mediumint" => ColumnTypeFamily::Int,
        "bigint" => ColumnTypeFamily::Int,
        "decimal" => ColumnTypeFamily::Float,
        "numeric" => ColumnTypeFamily::Float,
        "float" => ColumnTypeFamily::Float,
        "double" => ColumnTypeFamily::Float,
        "date" => ColumnTypeFamily::DateTime,
        "time" => ColumnTypeFamily::DateTime,
        "datetime" => ColumnTypeFamily::DateTime,
        "timestamp" => ColumnTypeFamily::DateTime,
        "year" => ColumnTypeFamily::DateTime,
        "char" => ColumnTypeFamily::String,
        "varchar" => ColumnTypeFamily::String,
        "text" => ColumnTypeFamily::String,
        "tinytext" => ColumnTypeFamily::String,
        "mediumtext" => ColumnTypeFamily::String,
        "longtext" => ColumnTypeFamily::String,
        // XXX: Is this correct?
        "enum" => ColumnTypeFamily::String,
        "set" => ColumnTypeFamily::String,
        "binary" => ColumnTypeFamily::Binary,
        "varbinary" => ColumnTypeFamily::Binary,
        "blob" => ColumnTypeFamily::Binary,
        "tinyblob" => ColumnTypeFamily::Binary,
        "mediumblob" => ColumnTypeFamily::Binary,
        "longblob" => ColumnTypeFamily::Binary,
        "geometry" => ColumnTypeFamily::Geometric,
        "point" => ColumnTypeFamily::Geometric,
        "linestring" => ColumnTypeFamily::Geometric,
        "polygon" => ColumnTypeFamily::Geometric,
        "multipoint" => ColumnTypeFamily::Geometric,
        "multilinestring" => ColumnTypeFamily::Geometric,
        "multipolygon" => ColumnTypeFamily::Geometric,
        "geometrycollection" => ColumnTypeFamily::Geometric,
        "json" => ColumnTypeFamily::Json,
        x => panic!(format!("type '{}' is not supported here yet.", x)),
    };
    ColumnType {
        raw: data_type.to_string(),
        family: family,
    }
}
