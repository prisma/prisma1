use super::*;
use ::postgres::Client;
use log::debug;
use prisma_query::ast::ParameterizedValue;
use prisma_query::connector::{PostgreSql, Queryable};
use std::borrow::Cow;
use std::collections::{HashMap, HashSet};

pub struct IntrospectionConnector {
    queryable: PostgreSql,
}

impl super::IntrospectionConnector for IntrospectionConnector {
    fn list_schemas(&self) -> Result<Vec<String>> {
        Ok(vec![])
    }

    fn introspect(&mut self, schema: &str) -> Result<DatabaseSchema> {
        debug!("Introspecting schema '{}'", schema);
        println!("Introspecting schema '{}'", schema);
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
    pub fn new(client: Client) -> Result<IntrospectionConnector> {
        let queryable = PostgreSql::from(client);
        Ok(IntrospectionConnector { queryable })
    }

    fn get_table_names(&mut self, schema: &str) -> Vec<String> {
        debug!("Getting table names");
        let sql = format!(
            "SELECT table_name as table_name FROM information_schema.tables
            WHERE table_schema = '{}'
            -- Views are not supported yet
            AND table_type = 'BASE TABLE'
        ORDER BY table_name",
            schema
        );
        let rows = self.queryable.query_raw(&sql, &[]).expect("get table names ");
        let names = rows
            .into_iter()
            .map(|row| {
                row.get("table_name")
                    .and_then(|x| x.to_string())
                    .expect("get table name")
            })
            .collect();

        debug!("Found table names: {:#?}", names);
        names
    }

    fn get_table(&mut self, schema: &str, name: &str) -> Table {
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

    fn get_columns(&mut self, schema: &str, table: &str) -> Vec<Column> {
        let sql = format!(
            "SELECT column_name, udt_name, column_default, is_nullable, 
            'false' as is_auto_increment
            FROM information_schema.columns
            WHERE table_schema = '{}' AND table_name  = '{}'
            ORDER BY column_name",
            schema, table
        );
        let rows = self.queryable.query_raw(&sql, &[]).expect("querying for columns");
        let cols = rows
            .into_iter()
            .map(|col| {
                debug!("Got column: {:#?}", col);
                let udt = col.get("udt_name").and_then(|x| x.to_string()).expect("get udt_name");
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
                            debug!("Converting default to string: {:#?}", x);
                            if x.is_null() {
                                None
                            } else {
                                let default = x.to_string().expect("default to string");
                                Some(default)
                            }
                        })
                        .expect("get default"),
                    auto_increment: None,
                }
            })
            .collect();

        debug!("Found table columns: {:#?}", cols);
        cols
    }

    fn get_foreign_keys(&mut self, schema: &str, table: &str) -> Vec<ForeignKey> {
        let sql = format!(
            "select 
                att2.attname as \"child_column\", 
                cl.relname as \"parent_table\", 
                att.attname as \"parent_column\"
            from
            (select 
                    unnest(con1.conkey) as \"parent\", 
                    unnest(con1.confkey) as \"child\", 
                    con1.confrelid, 
                    con1.conrelid,
                    con1.conname
                from 
                    pg_class cl
                    join pg_namespace ns on cl.relnamespace = ns.oid
                    join pg_constraint con1 on con1.conrelid = cl.oid
                where
                    cl.relname = '{}'
                    and ns.nspname = '{}'
                    and con1.contype = 'f'
            ) con
            join pg_attribute att on
                att.attrelid = con.confrelid and att.attnum = con.child
            join pg_class cl on
                cl.oid = con.confrelid
            join pg_attribute att2 on
                att2.attrelid = con.conrelid and att2.attnum = con.parent
            ",
            table, schema
        );
        debug!("Introspecting table foreign keys, SQL: '{}'", sql);
        let result_set = self.queryable.query_raw(&sql, &[]).expect("querying for foreign keys");
        result_set
            .into_iter()
            .map(|row| {
                debug!("Got row {:#?}", row);
                // TODO Handle multi-column
                let column = row
                    .get("child_column")
                    .and_then(|x| x.to_string())
                    .expect("get child_column");
                let columns = vec![column];
                // TODO Handle multi-column
                let referenced_column = row
                    .get("parent_column")
                    .and_then(|x| x.to_string())
                    .expect("get parent_column");
                let referenced_columns = vec![referenced_column];
                let fk = ForeignKey {
                    columns,
                    referenced_table: row
                        .get("parent_table")
                        .and_then(|x| x.to_string())
                        .expect("get parent_table"),
                    referenced_columns,
                };
                debug!(
                    "Found foreign key column(s): '{:?}', to table: '{}', to column(s): '{:?}'",
                    fk.columns, fk.referenced_table, fk.referenced_columns
                );
                fk
            })
            .collect()
    }

    fn get_indices(&mut self, schema: &str, table_name: &str) -> (Vec<Index>, Option<PrimaryKey>) {
        debug!("Getting indices");
        let sql = "SELECT indexInfos.relname as name,
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
            AND schemaInfo.nspname = $1::text
            AND tableInfos.relname = $2::text
            GROUP BY tableInfos.relname, indexInfos.relname, rawIndex.indisunique,
            rawIndex.indisprimary
        ";
        let rows = self
            .queryable
            .query_raw(
                &sql,
                &[
                    ParameterizedValue::Text(Cow::from(schema)),
                    ParameterizedValue::Text(Cow::from(table_name)),
                ],
            )
            .expect("querying for indices");
        let mut pk: Option<PrimaryKey> = None;
        let indices = rows
            .into_iter()
            .filter_map(|index| {
                debug!("Got index: {:#?}", index);
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
                    Some(Index {
                        name: index.get("name").and_then(|x| x.to_string()).expect("name"),
                        columns,
                        unique: index.get("is_unique").and_then(|x| x.as_bool()).expect("is_unique"),
                    })
                }
            })
            .collect();

        debug!("Found table indices: {:#?}, primary key: {:#?}", indices, pk);
        (indices, pk)
    }

    fn get_sequences(&mut self, schema: &str) -> Result<Vec<Sequence>> {
        debug!("Getting sequences");
        let sql = format!(
            "SELECT start_value, sequence_name
                  FROM information_schema.sequences
                  WHERE sequence_schema = '{}'
                  ",
            schema
        );
        let rows = self.queryable.query_raw(&sql, &[]).expect("querying for sequences");
        let sequences = rows
            .into_iter()
            .map(|seq| {
                debug!("Got sequence: {:#?}", seq);
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

        debug!("Found sequences: {:#?}", sequences);
        Ok(sequences)
    }

    fn get_enums(&mut self, schema: &str) -> Result<Vec<Enum>> {
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
        let rows = self.queryable.query_raw(&sql, &[]).expect("querying for enums");
        let mut enum_values: HashMap<String, HashSet<String>> = HashMap::new();
        for row in rows.into_iter() {
            debug!("Got enum row: {:#?}", row);
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
        debug!("Found enums: {:#?}", enums);
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
        "uuid" => ColumnTypeFamily::Uuid,
        // Array types
        "_bytea" => ColumnTypeFamily::Binary,
        "_bool" => ColumnTypeFamily::Boolean,
        "_date" => ColumnTypeFamily::DateTime,
        "_float8" => ColumnTypeFamily::Float,
        "_float4" => ColumnTypeFamily::Float,
        "_int4" => ColumnTypeFamily::Int,
        "_text" => ColumnTypeFamily::String,
        x => panic!(format!("type '{}' is not supported here yet.", x)),
    };
    ColumnType {
        raw: udt.to_string(),
        family: family,
    }
}
