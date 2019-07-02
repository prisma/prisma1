use super::database_inspector_impl::IntrospectedColumn;
use prisma_query::ast::ParameterizedValue;
use prisma_query::Connectional;
use std::sync::Arc;

pub struct InformationSchema {
    pub connectional: Arc<Connectional>,
    pub data_type_column: String,
}

impl InformationSchema {
    pub fn get_table_names(&self, schema: &str) -> Vec<String> {
        let sql = format!(
            r#"
            SELECT
                table_name
            FROM
                information_schema.tables
            WHERE
            table_schema = '{}' AND
            -- Views are not supported yet
            table_type = 'BASE TABLE'
        "#,
            schema
        );

        let result_set = self.connectional.query_on_raw_connection(&schema, &sql, &[]).unwrap();
        let names = result_set.into_iter().map(|row| row.at_as_string(0).unwrap()).collect();
        names
    }

    pub fn get_columns(&self, schema: &String, table: &String) -> Vec<IntrospectedColumn> {
        let sql = format!(
            r#"
            SELECT
                cols.ordinal_position,
                cols.column_name,
                cols.{} AS data_type,
                cols.column_default,
                cols.is_nullable = 'YES' as is_nullable
            FROM
                information_schema.columns AS cols
            WHERE
                cols.table_schema = '{}'
                AND cols.table_name  = '{}'
        "#,
            self.data_type_column, schema, table
        );

        let result_set = self.connectional.query_on_raw_connection(&schema, &sql, &[]).unwrap();
        let columns = result_set
            .into_iter()
            .map(|row| {
                let default_value = match row.get("column_default") {
                    Ok(ParameterizedValue::Text(v)) => Some(v.to_string()),
                    Ok(ParameterizedValue::Null) => None,
                    Ok(p) => panic!(format!("expectd a string value but got {:?}", p)),
                    Err(err) => panic!(format!("{}", err)),
                };
                IntrospectedColumn {
                    name: row.get_as_string("column_name").unwrap(),
                    table: table.to_string(),
                    tpe: row.get_as_string("data_type").unwrap(),
                    is_required: !row.get_as_bool("is_nullable").unwrap(),
                    default: default_value,
                    pk: 1 as u32, // TODO: implement foreign key llokup
                }
            })
            .collect();

        columns
    }

    pub fn get_primary_keys(&self, schema: &str, table: &str) -> Vec<String> {
        let sql = format!(
            r#"
            SELECT kcu.table_schema,
                kcu.table_name,
                tco.constraint_name,
                kcu.ordinal_position as position,
                kcu.column_name as key_column
            FROM information_schema.table_constraints tco
            JOIN information_schema.key_column_usage kcu 
                ON kcu.constraint_name = tco.constraint_name
                AND kcu.constraint_schema = tco.constraint_schema
                AND kcu.constraint_name = tco.constraint_name
            WHERE tco.constraint_type = 'PRIMARY KEY'
                AND kcu.table_schema = '{}'
                AND kcu.table_name = '{}'
            ORDER BY kcu.table_schema,
                    kcu.table_name,
                    position;
        "#,
            schema, table
        );

        let result_set = self.connectional.query_on_raw_connection(&schema, &sql, &[]).unwrap();
        let mut pks: Vec<String> = result_set
            .into_iter()
            .map(|row| row.get_as_string("key_column").unwrap())
            .collect();
        pks.dedup(); // this query yields duplicates on MySQL
        pks
    }
}
