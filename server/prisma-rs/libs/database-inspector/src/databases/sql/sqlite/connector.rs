use super::SqliteIntrospectionResult;
use crate::databases::sql::*;
use crate::{Connection, SqlError};
use prisma_query::{ast::*, convenience::*};

pub struct SqliteConnector {}

impl SqliteConnector {
    pub fn new() -> SqliteConnector {
        SqliteConnector {}
    }
}

fn result_list_to_string_vec(res: &ResultSet) -> Result<Vec<String>, SqlError> {
    let mut names: Vec<String> = vec![];

    for row in res.iter() {
        names.push(String::from(row.at_as_str(0)?))
    }

    Ok(names)
}

// TODO: Remove silencing as soon as impl finishes.
#[allow(unused)]

// TODO: Implement and test.
impl SpecializedSqlIntrospectionConnector for SqliteConnector {
    fn database_type(&self) -> &str {
        "sqlite"
    }
    fn list_schemas(&self, connection: &mut Connection) -> Result<Vec<String>, SqlError> {
        Ok(vec![])
    }

    fn query_tables(&self, connection: &mut Connection, schema: &str) -> Result<Vec<String>, SqlError> {
        let query = Select::from_table((schema, "sqlite_master"))
            .column("name")
            .so_that(ConditionTree::and(
                Column::from("type").equals("table"),
                Column::from("name").not_equals("sqlite_sequence"),
            ));

        let (cols, vals) = connection.query(Query::from(query))?;
        let res = ResultSet::from((&cols, &vals));

        result_list_to_string_vec(&res)
    }

    fn query_relations(&self, connection: &mut Connection, schema: &str) -> Result<Vec<TableRelationInfo>, SqlError> {
        let mut rels: Vec<TableRelationInfo> = vec![];

        for table in self.query_tables(connection, schema)? {
            let sql = format!(r#"Pragma "{}".foreign_key_list("{}");"#, schema, table);
            let (cols, vals) = connection.query_raw(&sql, &[])?;
            let res = ResultSet::from((&cols, &vals));

            // Pragma foreign_key_list: id|seq|table|from|to|on_update|on_delete|match
            // prisma-query currently cannot access columns by name.
            const PRAGMA_TABLE: &str = "table";
            const PRAGMA_FROM: &str = "from";
            const PRAGMA_TO: &str = "to";

            for row in res.iter() {
                rels.push(TableRelationInfo {
                    source_table: table.clone(),
                    target_table: row.get_as_string(PRAGMA_TABLE)?,
                    source_column: row.get_as_string(PRAGMA_FROM)?,
                    target_column: row.get_as_string(PRAGMA_TO)?,
                })
            }
        }

        Ok(rels)
    }

    fn query_columns(
        &self,
        connection: &mut Connection,
        schema: &str,
        table: &str,
    ) -> Result<Vec<ColumnInfo>, SqlError> {
        let sql = format!(r#"Pragma `{}`.table_info (`{}`)"#, schema, table);
        let (cols, vals) = connection.query_raw(&sql, &[])?;
        let res = ResultSet::from((&cols, &vals));

        let mut cols: Vec<ColumnInfo> = vec![];

        // Pragma table_info output: cid|name|type|notnull|dflt_value|pk
        // prisma-query currently cannot access columns by name.
        const PRAGMA_PK: &str = "pk";
        const PRAGMA_NOT_NULL: &str = "notnull";
        const PRAGMA_DEFAULT: &str = "dflt_value";
        const PRAGMA_TYPE: &str = "type";
        const PRAGMA_NAME: &str = "name";

        for row in res.iter() {
            cols.push(ColumnInfo {
                name: row.get_as_string(PRAGMA_NAME)?,
                is_unique: false,
                default_value: row.get_as_string(PRAGMA_DEFAULT).ok(),
                column_type: self.native_type_to_column_type(&row.get_as_string(PRAGMA_TYPE)?),
                comment: None,
                is_nullable: !(row.get_as_bool(PRAGMA_NOT_NULL)?),
                is_list: false,
                is_auto_increment: row.get_as_bool(PRAGMA_PK)?,
                is_primary_key: row.get_as_bool(PRAGMA_PK)?,
            })
        }

        Ok(cols)
    }
    fn query_column_comment(
        &self,
        connection: &mut Connection,
        schema: &str,
        table: &str,
        column: &str,
    ) -> Result<Option<String>, SqlError> {
        // TODO: Implement
        Ok(None)
    }
    fn query_indices(
        &self,
        connection: &mut Connection,
        schema: &str,
        table: &str,
    ) -> Result<Vec<InternalIndexIntrospectionResult>, SqlError> {
        // TODO: This is a hack which exposes only the primary key of each table.
        // https://github.com/prisma/prisma/blob/27002e24774a17f4aea8a2d8d0ec834ce84d4c8d/server/prisma-rs/libs/database-inspector/src/database_inspector_impl.rs#L59

        let pk_cols: Vec<String> = self
            .query_columns(connection, schema, table)?
            .iter()
            .filter(|c| c.is_primary_key)
            .map(|c| c.name.clone())
            .collect();

        if pk_cols.len() > 0 {
            return Ok(vec![InternalIndexIntrospectionResult {
                is_unique: true,
                is_primary_key: true,
                table_name: String::from(table),
                name: String::from("pk"),
                columns: pk_cols,
            }]);
        } else {
            return Ok(vec![]);
        }
    }
    fn query_enums(&self, connection: &mut Connection, schema: &str) -> Result<Vec<EnumInfo>, SqlError> {
        // TODO: Implement
        Ok(vec![])
    }
    fn query_sequences(&self, connection: &mut Connection, schema: &str) -> Result<Vec<SequenceInfo>, SqlError> {
        // TODO: Implement
        Ok(vec![])
    }

    fn create_introspection_result(
        &self,
        tables: Vec<TableInfo>,
        relations: Vec<TableRelationInfo>,
        enums: Vec<EnumInfo>,
        sequences: Vec<SequenceInfo>,
    ) -> SqlIntrospectionResult {
        SqlIntrospectionResult {
            specialized: Box::new(SqliteIntrospectionResult::new()),
            schema: DatabaseSchemaInfo {
                tables: tables,
                relations: relations,
                enums: enums,
                sequences: sequences,
            },
        }
    }

    fn column_type_to_native_type(&self, col: &ColumnType) -> &str {
        match col {
            ColumnType::Boolean => "BOOLEAN",
            ColumnType::DateTime => "DATE",
            ColumnType::Float => "REAL",
            ColumnType::Int => "INTEGER",
            ColumnType::String => "TEXT",
        }
    }

    fn native_type_to_column_type(&self, col: &str) -> ColumnType {
        match col {
            "BOOLEAN" => ColumnType::Boolean,
            "DATE" => ColumnType::DateTime,
            "REAL" => ColumnType::Float,
            "INTEGER" => ColumnType::Int,
            "TEXT" => ColumnType::String,
            // Everything else is text.
            _ => ColumnType::String,
        }
    }
}
