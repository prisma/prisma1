use super::*;
use ::postgres::Client;
use log::debug;
use prisma_query::connector::{PostgreSql, Queryable};
use std::collections::HashMap;

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
        Ok(DatabaseSchema {
            enums: vec![],
            sequences: vec![],
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
        let (columns, primary_key) = self.get_columns(schema, name);
        let foreign_keys = self.get_foreign_keys(schema, name);
        Table {
            name: name.to_string(),
            columns,
            foreign_keys,
            indexes: vec![],
            primary_key: None,
        }
    }

    fn get_columns(&mut self, schema: &str, table: &str) -> (Vec<Column>, Option<PrimaryKey>) {
        let sql = format!(
            "SELECT ordinal_position, column_name, udt_name,
            column_default, is_nullable = 'YES' as is_nullable, 'false' as is_auto_increment
            FROM information_schema.columns
            WHERE table_schema = '{}' AND table_name  = '{}'
            ORDER BY column_name",
            schema, table
        );
        // Note that ordinal_position comes back as a string because it's a bigint
        let rows = self.queryable.query_raw(&sql, &[]).expect("querying for columns");

        let mut pk_cols: HashMap<i64, String> = HashMap::new();
        let cols = rows
            .into_iter()
            .map(|col| {
                debug!("Got column: {:#?}", col);
                let udt = col.get("udt_name").and_then(|x| x.to_string()).expect("get udt_name");
                // if col.pk > 0 {
                //     pk_cols.insert(col.pk, col.name.clone());
                // }
                Column {
                    name: col
                        .get("column_name")
                        .and_then(|x| x.to_string())
                        .expect("get column name"),
                    tpe: get_column_type(udt.as_ref()),
                    arity: col
                        .get("is_nullable")
                        .map(|x| {
                            let is_nullable = x.as_bool().expect("is_nullable");
                            if is_nullable {
                                ColumnArity::Nullable
                            } else {
                                ColumnArity::Required
                            }
                        })
                        .expect("get is_nullable"),
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
        (cols, None)
    }

    fn get_foreign_keys(&mut self, schema: &str, table: &str) -> Vec<ForeignKey> {
        vec![]
        // let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);
        // debug!("Introspecting table foreign keys, SQL: '{}'", sql);
        // let result_set = self.queryable.query_raw(&sql, &[]).expect("querying for foreign keys");
        // result_set
        //     .into_iter()
        //     .map(|row| {
        //         let fk = ForeignKey {
        //             column: row.get("from").and_then(|x| x.to_string()).expect("from"),
        //             referenced_table: row.get("table").and_then(|x| x.to_string()).expect("table"),
        //             referenced_column: row.get("to").and_then(|x| x.to_string()).expect("to"),
        //         };
        //         debug!(
        //             "Found foreign key column: '{}', to table: '{}', to column: '{}'",
        //             fk.column, fk.referenced_table, fk.referenced_column
        //         );
        //         fk
        //     })
        //     .collect()
    }

    fn get_indices(&mut self, schema: &str, table_name: &str) -> (Vec<Index>, Option<PrimaryKey>) {
        let sql = "SELECT indexInfos.relname as name,
            array_to_string(array_agg(columnInfos.attname), ',') as column_names,
            rawIndex.indisunique as is_unique, rawIndex.indisprimary as is_primary_key
            FROM
            -- pg_class stores infos about tables, indices etc: https://www.postgresql.org/docs/9.3/catalog-pg-class.html
            pg_class tableInfos, pg_class indexInfos,
            -- pg_index stores indices: https://www.postgresql.org/docs/9.3/catalog-pg-index.html
            pg_index rawIndex,
            -- pg_attribute stores infos about columns: https://www.postgresql.org/docs/9.3/catalog-pg-attribute.html
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
        let rows = self.queryable.query_raw(&sql, &[]).expect("querying for indices");

        let indices = rows
            .into_iter()
            .map(|index| {
                debug!("Got index: {:#?}", index);
                Index {
                    name: index.get("name").and_then(|x| x.to_string()).expect("name"),
                    columns: vec![], //index.get("column_names").and_then(|x| x.into_vec::<String>()).expect("column_names"),
                    unique: index.get("is_unique").and_then(|x| x.as_bool()).expect("is_unique"),
                }
            })
            .collect();

        debug!("Found table indices: {:#?}", indices);
        (indices, None)

        // return (await this.query(indexQuery, [schemaName, tableName])).map(row => {
        //     return {
        //         table_name,
        //         name: row.index_name as string,
        //         fields: this.parseJoinedArray(row.column_names),
        //         unique: row.is_unique as boolean,
        //         isPrimaryKey: row.is_primary_key as boolean,
        //     }
        // })
    }
}

fn get_column_type(udt: &str) -> ColumnType {
    let family = match udt {
        "int2" => ColumnTypeFamily::Int,
        "int4" => ColumnTypeFamily::Int,
        "int8" => ColumnTypeFamily::Int,
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
        x => panic!(format!("type '{}' is not supported here yet.", x)),
    };
    ColumnType {
        raw: udt.to_string(),
        family: family,
    }
}
