use super::database_inspector_impl::{convert_introspected_columns, IntrospectedForeignKey};
use super::information_schema::InformationSchema;
use super::*;
use prisma_query::Connectional;
use std::sync::Arc;

pub struct MysqlInspector {
    pub connectional: Arc<Connectional>,
    information_schema: InformationSchema,
}

impl DatabaseInspector for MysqlInspector {
    fn introspect(&self, schema: &String) -> DatabaseSchema {
        DatabaseSchema {
            tables: self
                .information_schema
                .get_table_names(schema)
                .into_iter()
                .map(|t| self.get_table(schema, &t))
                .collect(),
        }
    }
}

impl MysqlInspector {
    pub fn new(connectional: Arc<Connectional>) -> MysqlInspector {
        MysqlInspector {
            connectional: Arc::clone(&connectional),
            information_schema: InformationSchema {
                connectional: Arc::clone(&connectional),
                data_type_column: "DATA_TYPE".to_string(),
            },
        }
    }

    fn get_table(&self, schema: &String, table: &String) -> Table {
        let introspected_columns = self.information_schema.get_columns(&schema, &table);
        let introspected_foreign_keys = self.get_foreign_key_constraints(&schema, &table);
        let primary_key_columns = self.information_schema.get_primary_keys(&schema, &table);
        Table {
            name: table.to_string(),
            columns: convert_introspected_columns(
                introspected_columns,
                introspected_foreign_keys,
                Box::new(column_type),
            ),
            indexes: Vec::new(),
            primary_key_columns: primary_key_columns,
        }
    }

    fn get_foreign_key_constraints(&self, schema: &String, table: &String) -> Vec<IntrospectedForeignKey> {
        /*
        sql"""
         |SELECT
         |    kcu.constraint_name AS fkConstraintName,
         |    kcu.table_name AS fkTablename,
         |    kcu.column_name AS fkColumnName,
         |    kcu.referenced_table_name AS referencedTableName,
         |    kcu.referenced_column_name AS referencedColumnName
         |FROM
         |    information_schema.key_column_usage kcu
         |WHERE
         |    kcu.table_schema  = $schema
         |    AND kcu.referenced_table_name IS NOT NULL;
            """.stripMargin.as[IntrospectedForeignKey]
        */
        let sql = format!(
            r#"
            SELECT
	            kcu.constraint_name as "fkConstraintName",
                kcu.table_name as "fkTableName",
                kcu.column_name as "fkColumnName",
                ccu.table_name as "referencedTableName",
                ccu.column_name as "referencedColumnName"
            FROM
                information_schema.key_column_usage kcu
            INNER JOIN
                information_schema.constraint_column_usage AS ccu
                ON ccu.constraint_catalog = kcu.constraint_catalog
                AND ccu.constraint_schema = kcu.constraint_schema
                AND ccu.constraint_name = kcu.constraint_name
            INNER JOIN
                information_schema.referential_constraints as rc
                ON rc.constraint_catalog = kcu.constraint_catalog
                AND rc.constraint_schema = kcu.constraint_schema
                AND rc.constraint_name = kcu.constraint_name
            WHERE
                kcu.table_schema = '{}' AND
                kcu.table_name = '{}'
        "#,
            schema, table
        );

        let result_set = self.connectional.query_on_raw_connection(&schema, &sql, &[]).unwrap();
        result_set
            .into_iter()
            .map(|row| IntrospectedForeignKey {
                name: row.get_as_string("fkConstraintName").unwrap(),
                table: row.get_as_string("fkTableName").unwrap(),
                column: row.get_as_string("fkColumnName").unwrap(),
                referenced_table: row.get_as_string("referencedTableName").unwrap(),
                referenced_column: row.get_as_string("referencedColumnName").unwrap(),
            })
            .collect()
    }
}

fn column_type(column: &IntrospectedColumn) -> ColumnType {
    // https://dev.mysql.com/doc/refman/8.0/en/data-types.html
    match column.tpe.as_ref() {
        "tinyint" => ColumnType::Boolean,
        s if s.contains("varchar") => ColumnType::String,
        s if s.contains("text") => ColumnType::String,
        s if s.contains("int") => ColumnType::Int,
        "decimal" | "numeric" | "float" | "double" => ColumnType::Float,
        "datetime" | "timestamp" => ColumnType::DateTime,
        x => panic!(format!(
            "type {} is not supported here yet. Column was: {}",
            x, column.name
        )),
    }
}