use crate::relational::SpecializedRelationalIntrospectionConnector;
use prisma_query::{ResultRow, ast::*};
use crate::{Connection, SqlError};
use crate::databases::relational::*;
use super::SqlLiteIntrospectionResult;

pub struct SqlLiteConnector { }

impl SqlLiteConnector { 
    pub fn new() -> SqlLiteConnector {
      SqlLiteConnector { }
    }
}

fn result_list_to_string_vec(res: &Vec<ResultRow>) -> Result<Vec<String>, SqlError> {
    let mut names: Vec<String> = vec![];

    for row in res {
        names.push(String::from(row.as_str(0)?))
    }

    Ok(names)
}

// TODO: Implement and test.
impl SpecializedRelationalIntrospectionConnector for SqlLiteConnector {
    fn database_type(&self) -> &str {
        "sqlite"
    }
    fn list_schemas(&self, connection: &mut Connection) -> Result<Vec<String>, SqlError> {
        Ok(vec![])
    }

    fn query_tables(&self, connection: &mut Connection, schema: &str) -> Result<Vec<String>, SqlError> {
        let query = Select::from_table((schema, "sqlite_master"))
            .column("name")
            .so_that(Column::from("type").equals("table"));

        let res = connection.query(Query::from(query))?;

        result_list_to_string_vec(&res)
    }
    
    fn query_relations(&self, connection: &mut Connection, schema: &str) -> Result<Vec<TableRelationInfo>, SqlError> {
        let mut rels: Vec<TableRelationInfo> = vec![];

        for table in self.query_tables(connection, schema)? {
            let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);
            let res = connection.query_raw(&sql, &[])?;

            // Pragma foreign_key_list: id|seq|table|from|to|on_update|on_delete|match
            // prisma-query currently cannot access columns by name.
            const PRAGMA_TABLE: usize = 2;
            const PRAGMA_FROM: usize = 3;
            const PRAGMA_TO: usize = 4;

            for row in res {
                rels.push(TableRelationInfo {
                    source_table: table.clone(),
                    target_table: row.as_string(PRAGMA_TABLE)?,
                    source_column: row.as_string(PRAGMA_FROM)?,
                    target_column: row.as_string(PRAGMA_TO)?,
                })
            }
        }

        Ok(rels)
    }

    fn query_columns(&self, connection: &mut Connection, schema: &str, table: &str) -> Result<Vec<ColumnInfo>, SqlError> {
        let sql = format!(r#"Pragma `{}`.table_info (`{}`)"#, schema, table);
        let res = connection.query_raw(&sql, &[])?;

        let mut cols: Vec<ColumnInfo> = vec![];

        // Pragma table_info output: cid|name|type|notnull|dflt_value|pk
        // prisma-query currently cannot access columns by name.
        const PRAGMA_PK: usize = 5;
        const PRAGMA_NOT_NULL: usize = 3;
        const PRAGMA_DEFAULT: usize = 4;
        const PRAGMA_TYPE: usize = 2;
        const PRAGMA_NAME: usize = 1;

        for row in res {
            cols.push(ColumnInfo {
                name: row.as_string(PRAGMA_NAME)?,
                is_unique: false,
                default_value: row.as_string(PRAGMA_DEFAULT).ok(),
                column_type: row.as_string(PRAGMA_TYPE)?,
                comment: None,
                is_nullable: !(row.as_bool(PRAGMA_NOT_NULL)?),
                is_list: false,
                is_auto_increment: row.as_bool(PRAGMA_PK)?,
                is_primary_key: row.as_bool(PRAGMA_PK)?
            })
        }

        Ok(cols)
    }
    fn query_column_comment(&self, connection: &mut Connection, schema: &str, table: &str, column: &str) -> Result<Option<String>, SqlError> {
        // TODO: Implement
        Ok(None)
    }
    fn query_indices(&self, connection: &mut Connection, schema: &str, table: &str) -> Result<Vec<InternalIndexIntrospectionResult>, SqlError> {
        // TODO: Implement
        Ok(vec![])
    }
    fn query_enums(&self, connection: &mut Connection, schema: &str) -> Result<Vec<EnumInfo>, SqlError> {
        // TODO: Implement
        Ok(vec![])
    }
    fn query_sequences(&self, connection: &mut Connection, schema: &str) -> Result<Vec<SequenceInfo>, SqlError> {
        // TODO: Implement
        Ok(vec![])
    }

    fn create_introspection_result(&self, models: Vec<TableInfo>, relations: Vec<TableRelationInfo>, enums: Vec<EnumInfo>, sequences: Vec<SequenceInfo>) -> RelationalIntrospectionResult{
        RelationalIntrospectionResult {
            specialized: Box::new(SqlLiteIntrospectionResult::new()),
            schema: SchemaInfo {
                models: models,
                relations: relations,
                enums: enums,
                sequences: sequences
            }
        }
    }

}