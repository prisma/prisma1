//! Postgres introspection.
use super::*;
use log::debug;
use std::collections::{HashMap, HashSet};
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
        let enums = self.get_enums(schema)?;
        let sequences = self.get_sequences(schema)?;
        Ok(DatabaseSchema {
            enums,
            sequences,
            tables,
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
        let (indices, primary_key) = self.get_indices(schema, name);
        let foreign_keys = self.get_foreign_keys(schema, name);
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
            "SELECT column_name, udt_name, column_default, is_nullable, is_identity, data_type
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
                let col_name = col
                    .get("column_name")
                    .and_then(|x| x.to_string())
                    .expect("get column name");
                let udt = col.get("udt_name").and_then(|x| x.to_string()).expect("get udt_name");
                let is_identity_str = col
                    .get("is_identity")
                    .and_then(|x| x.to_string())
                    .expect("get is_identity")
                    .to_lowercase();
                let is_identity = match is_identity_str.as_str() {
                    "no" => false,
                    "yes" => true,
                    _ => panic!("unrecognized is_identity variant '{}'", is_identity_str),
                };
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
                let tpe = get_column_type(udt.as_ref());
                let arity = if tpe.raw.starts_with("_") {
                    ColumnArity::List
                } else if is_required {
                    ColumnArity::Required
                } else {
                    ColumnArity::Nullable
                };
                let default = col
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
                    .expect("get default");
                let is_auto_increment = is_identity
                    || match default {
                        Some(ref val) => {
                            val == &format!("nextval(\'\"{}\".\"{}_{}_seq\"\'::regclass)", schema, table, col_name,)
                        }
                        None => false,
                    };
                Column {
                    name: col_name,
                    tpe,
                    arity,
                    default,
                    auto_increment: is_auto_increment,
                }
            })
            .collect();

        debug!("Found table columns: {:?}", cols);
        cols
    }

    fn get_foreign_keys(&self, schema: &str, table: &str) -> Vec<ForeignKey> {
        let sql = format!(
            "SELECT 
                con.oid as \"con_id\",
                att2.attname as \"child_column\", 
                cl.relname as \"parent_table\", 
                att.attname as \"parent_column\",
                con.confdeltype
            FROM
            (SELECT 
                    unnest(con1.conkey) as \"parent\", 
                    unnest(con1.confkey) as \"child\", 
                    con1.oid,
                    con1.confrelid, 
                    con1.conrelid,
                    con1.conname,
                    con1.confdeltype
                FROM
                    pg_class cl
                    join pg_namespace ns on cl.relnamespace = ns.oid
                    join pg_constraint con1 on con1.conrelid = cl.oid
                WHERE
                    cl.relname = '{}'
                    and ns.nspname = '{}'
                    and con1.contype = 'f'
            ) con
            JOIN pg_attribute att on
                att.attrelid = con.confrelid and att.attnum = con.child
            JOIN pg_class cl on
                cl.oid = con.confrelid
            JOIN pg_attribute att2 on
                att2.attrelid = con.conrelid and att2.attnum = con.parent
            ORDER BY con_id
            ",
            table, schema
        );
        debug!("Introspecting table foreign keys, SQL: '{}'", sql);

        // One foreign key with multiple columns will be represented here as several
        // rows with the same ID, which we will have to combine into corresponding foreign key
        // objects.
        let result_set = self.conn.query_raw(&sql, schema).expect("querying for foreign keys");
        let mut intermediate_fks: HashMap<i64, ForeignKey> = HashMap::new();
        for row in result_set.into_iter() {
            debug!("Got introspection FK row {:?}", row);
            let id = row.get("con_id").and_then(|x| x.as_i64()).expect("get con_id");
            let column = row
                .get("child_column")
                .and_then(|x| x.to_string())
                .expect("get child_column");
            let referenced_table = row
                .get("parent_table")
                .and_then(|x| x.to_string())
                .expect("get parent_table");
            let referenced_column = row
                .get("parent_column")
                .and_then(|x| x.to_string())
                .expect("get parent_column");
            let confdeltype = row
                .get("confdeltype")
                .and_then(|x| x.as_char())
                .expect("get confdeltype");
            let on_delete_action = match confdeltype {
                'a' => ForeignKeyAction::NoAction,
                'r' => ForeignKeyAction::Restrict,
                'c' => ForeignKeyAction::Cascade,
                'n' => ForeignKeyAction::SetNull,
                'd' => ForeignKeyAction::SetDefault,
                _ => panic!(format!("unrecognized foreign key action '{}'", confdeltype)),
            };
            match intermediate_fks.get_mut(&id) {
                Some(fk) => {
                    fk.columns.push(column);
                    fk.referenced_columns.push(referenced_column);
                }
                None => {
                    let fk = ForeignKey {
                        columns: vec![column],
                        referenced_table,
                        referenced_columns: vec![referenced_column],
                        on_delete_action,
                    };
                    intermediate_fks.insert(id, fk);
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

    fn get_indices(&self, schema: &str, table_name: &str) -> (Vec<Index>, Option<PrimaryKey>) {
        debug!("Getting indices");
        let sql = format!("SELECT indexInfos.relname as name,
            array_agg(columnInfos.attname) as column_names,
            rawIndex.indisunique as is_unique, rawIndex.indisprimary as is_primary_key
            FROM
            -- pg_class stores infos about tables, indices etc: https://www.postgresql.org/docs/current/catalog-pg-class.html
            pg_class tableInfos, pg_class indexInfos,
            -- pg_index stores indices: https://www.postgresql.org/docs/current/catalog-pg-index.html
            pg_index rawIndex,
            -- pg_attribute stores infos about columns: https://www.postgresql.org/docs/current/catalog-pg-attribute.html
            pg_attribute columnInfos,
            -- pg_namespace stores info about the schema
            pg_namespace schemaInfo
            WHERE
            -- find table info for index
            tableInfos.oid = rawIndex.indrelid
            -- find index info
            AND indexInfos.oid = rawIndex.indexrelid
            -- find table columns
            AND columnInfos.attrelid = tableInfos.oid
            AND columnInfos.attnum = ANY(rawIndex.indkey)
            -- we only consider ordinary tables
            AND tableInfos.relkind = 'r'
            -- we only consider stuff out of one specific schema
            AND tableInfos.relnamespace = schemaInfo.oid
            AND schemaInfo.nspname = '{}'
            AND tableInfos.relname = '{}'
            GROUP BY tableInfos.relname, indexInfos.relname, rawIndex.indisunique,
            rawIndex.indisprimary
        ", schema, table_name);
        let rows = self.conn.query_raw(&sql, schema).expect("querying for indices");
        let mut pk: Option<PrimaryKey> = None;
        let indices = rows
            .into_iter()
            .filter_map(|index| {
                debug!("Got index: {:?}", index);
                let is_pk = index
                    .get("is_primary_key")
                    .and_then(|x| x.as_bool())
                    .expect("get is_primary_key");
                // TODO: Implement and use as_slice instead of into_vec, to avoid cloning
                let columns = index
                    .get("column_names")
                    .and_then(|x| x.clone().into_vec::<String>())
                    .expect("column_names");
                if is_pk {
                    pk = Some(PrimaryKey { columns });
                    None
                } else {
                    let is_unique = index.get("is_unique").and_then(|x| x.as_bool()).expect("is_unique");
                    Some(Index {
                        name: index.get("name").and_then(|x| x.to_string()).expect("name"),
                        columns,
                        tpe: match is_unique {
                            true => IndexType::Unique,
                            false => IndexType::Normal,
                        },
                    })
                }
            })
            .collect();

        debug!("Found table indices: {:?}, primary key: {:?}", indices, pk);
        (indices, pk)
    }

    fn get_sequences(&self, schema: &str) -> IntrospectionResult<Vec<Sequence>> {
        debug!("Getting sequences");
        let sql = format!(
            "SELECT start_value, sequence_name
                  FROM information_schema.sequences
                  WHERE sequence_schema = '{}'
                  ",
            schema
        );
        let rows = self.conn.query_raw(&sql, schema).expect("querying for sequences");
        let sequences = rows
            .into_iter()
            .map(|seq| {
                debug!("Got sequence: {:?}", seq);
                let initial_value = seq
                    .get("start_value")
                    .and_then(|x| x.to_string())
                    .and_then(|x| x.parse::<u32>().ok())
                    .expect("get start_value");
                Sequence {
                    // Not sure what allocation size refers to, but the TypeScript implementation
                    // hardcodes this as 1
                    allocation_size: 1,
                    initial_value,
                    name: seq
                        .get("sequence_name")
                        .and_then(|x| x.to_string())
                        .expect("get sequence_name"),
                }
            })
            .collect();

        debug!("Found sequences: {:?}", sequences);
        Ok(sequences)
    }

    fn get_enums(&self, schema: &str) -> IntrospectionResult<Vec<Enum>> {
        debug!("Getting enums");
        let sql = format!(
            "SELECT t.typname as name, e.enumlabel as value
            FROM pg_type t 
            JOIN pg_enum e ON t.oid = e.enumtypid  
            JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
            WHERE n.nspname = '{}'
            ",
            schema
        );
        let rows = self.conn.query_raw(&sql, schema).expect("querying for enums");
        let mut enum_values: HashMap<String, HashSet<String>> = HashMap::new();
        for row in rows.into_iter() {
            debug!("Got enum row: {:?}", row);
            let name = row.get("name").and_then(|x| x.to_string()).expect("get name");
            let value = row.get("value").and_then(|x| x.to_string()).expect("get value");
            if !enum_values.contains_key(&name) {
                enum_values.insert(name.clone(), HashSet::new());
            }
            let vals = enum_values.get_mut(&name).expect("get enum values");
            vals.insert(value);
        }

        let enums: Vec<Enum> = enum_values
            .into_iter()
            .map(|(k, v)| Enum { name: k, values: v })
            .collect();
        debug!("Found enums: {:?}", enums);
        Ok(enums)
    }
}

fn get_column_type(udt: &str) -> ColumnType {
    let family = match udt {
        "int2" => ColumnTypeFamily::Int,
        "int4" => ColumnTypeFamily::Int,
        "int8" => ColumnTypeFamily::Int,
        "float4" => ColumnTypeFamily::Float,
        "float8" => ColumnTypeFamily::Float,
        "bool" => ColumnTypeFamily::Boolean,
        "text" => ColumnTypeFamily::String,
        "varchar" => ColumnTypeFamily::String,
        "date" => ColumnTypeFamily::DateTime,
        "bytea" => ColumnTypeFamily::Binary,
        "json" => ColumnTypeFamily::Json,
        "jsonb" => ColumnTypeFamily::Json,
        "uuid" => ColumnTypeFamily::Uuid,
        "bit" => ColumnTypeFamily::Binary,
        "varbit" => ColumnTypeFamily::Binary,
        "box" => ColumnTypeFamily::Geometric,
        "circle" => ColumnTypeFamily::Geometric,
        "line" => ColumnTypeFamily::Geometric,
        "lseg" => ColumnTypeFamily::Geometric,
        "path" => ColumnTypeFamily::Geometric,
        "polygon" => ColumnTypeFamily::Geometric,
        "bpchar" => ColumnTypeFamily::String,
        "interval" => ColumnTypeFamily::DateTime,
        "numeric" => ColumnTypeFamily::Float,
        "pg_lsn" => ColumnTypeFamily::LogSequenceNumber,
        "time" => ColumnTypeFamily::DateTime,
        "timetz" => ColumnTypeFamily::DateTime,
        "timestamp" => ColumnTypeFamily::DateTime,
        "timestamptz" => ColumnTypeFamily::DateTime,
        "tsquery" => ColumnTypeFamily::TextSearch,
        "tsvector" => ColumnTypeFamily::TextSearch,
        "txid_snapshot" => ColumnTypeFamily::TransactionId,
        // Array types
        "_bytea" => ColumnTypeFamily::Binary,
        "_bool" => ColumnTypeFamily::Boolean,
        "_date" => ColumnTypeFamily::DateTime,
        "_float8" => ColumnTypeFamily::Float,
        "_float4" => ColumnTypeFamily::Float,
        "_int4" => ColumnTypeFamily::Int,
        "_text" => ColumnTypeFamily::String,
        "_varchar" => ColumnTypeFamily::String,
        x => panic!(format!("type '{}' is not supported here yet.", x)),
    };
    ColumnType {
        raw: udt.to_string(),
        family: family,
    }
}
