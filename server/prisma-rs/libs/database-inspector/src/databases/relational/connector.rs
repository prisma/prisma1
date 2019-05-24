use crate::relational::*;
use crate::*;
use crate::{Connection, SqlError};

pub trait SpecializedRelationalIntrospectionConnector {
    fn database_type(&self) -> &str;
    fn list_schemas(&self, connection: &mut Connection) -> Result<Vec<String>, SqlError>;
    fn query_tables(&self, connection: &mut Connection, schema: &str) -> Result<Vec<String>, SqlError>;
    fn query_relations(&self, connection: &mut Connection, schema: &str) -> Result<Vec<TableRelationInfo>, SqlError>;
    fn query_columns(
        &self,
        connection: &mut Connection,
        schema: &str,
        table: &str,
    ) -> Result<Vec<ColumnInfo>, SqlError>;
    fn query_column_comment(
        &self,
        connection: &mut Connection,
        schema: &str,
        table: &str,
        column: &str,
    ) -> Result<Option<String>, SqlError>;
    fn query_indices(
        &self,
        connection: &mut Connection,
        schema: &str,
        table: &str,
    ) -> Result<Vec<InternalIndexIntrospectionResult>, SqlError>;
    fn query_enums(&self, connection: &mut Connection, schema: &str) -> Result<Vec<EnumInfo>, SqlError>;
    fn query_sequences(&self, connection: &mut Connection, schema: &str) -> Result<Vec<SequenceInfo>, SqlError>;
    fn create_introspection_result(
        &self,
        tables: Vec<TableInfo>,
        relations: Vec<TableRelationInfo>,
        enums: Vec<EnumInfo>,
        sequences: Vec<SequenceInfo>,
    ) -> RelationalIntrospectionResult;
    fn column_type_to_native_type(&self, col: &ColumnType) -> &str;
    fn native_type_to_column_type(&self, col: &str) -> ColumnType;
}

pub struct RelationalIntrospectionConnector {
    specialized: Box<SpecializedRelationalIntrospectionConnector>,
}

impl RelationalIntrospectionConnector {
    pub fn new(specialized: Box<SpecializedRelationalIntrospectionConnector>) -> RelationalIntrospectionConnector {
        RelationalIntrospectionConnector { specialized }
    }

    fn list_tables(&self, connection: &mut Connection, schema: &str) -> Result<Vec<TableInfo>, SqlError> {
        let mut tables: Vec<TableInfo> = vec![];
        let all_tables = self.specialized.query_tables(connection, schema)?;

        for table_name in &all_tables {
            let mut columns = self.specialized.query_columns(connection, schema, table_name)?;

            for column in &mut columns {
                column.comment = self
                    .specialized
                    .query_column_comment(connection, schema, table_name, &column.name)?;
            }

            let all_indices = self.specialized.query_indices(connection, schema, table_name)?;
            let secondary_inidices = all_indices
                .iter()
                .filter(|i| !i.is_primary_key)
                .map(|i| IndexInfo::from(i.clone()))
                .collect();
            let primary_key = all_indices
                .iter()
                .find(|i| i.is_primary_key)
                .map(|i| IndexInfo::from(i.clone()));

            tables.push(TableInfo {
                name: table_name.clone(),
                columns: columns,
                indexes: secondary_inidices,
                primary_key: primary_key,
            })
        }

        Ok(tables)
    }

    fn list_relations(&self, connection: &mut Connection, schema: &str) -> Result<Vec<TableRelationInfo>, SqlError> {
        self.specialized.query_relations(connection, schema)
    }

    fn list_enums(&self, connection: &mut Connection, schema: &str) -> Result<Vec<EnumInfo>, SqlError> {
        self.specialized.query_enums(connection, schema)
    }

    fn list_sequences(&self, connection: &mut Connection, schema: &str) -> Result<Vec<SequenceInfo>, SqlError> {
        self.specialized.query_sequences(connection, schema)
    }

    pub fn column_type_to_native_type(&self, col: &ColumnType) -> &str {
        self.specialized.column_type_to_native_type(col)
    }

    pub fn native_type_to_column_type(&self, col: &str) -> ColumnType {
        self.specialized.native_type_to_column_type(col)

    }
}

impl IntrospectionConnector<RelationalIntrospectionResult> for RelationalIntrospectionConnector {
    fn database_type(&self) -> &str {
        self.specialized.database_type()
    }

    fn list_schemas(&self, connection: &mut Connection) -> Result<Vec<String>, SqlError> {
        self.specialized.list_schemas(connection)
    }

    fn introspect(&self, connection: &mut Connection, schema: &str) -> Result<RelationalIntrospectionResult, SqlError> {
        Ok(self.specialized.create_introspection_result(
            self.list_tables(connection, schema)?,
            self.list_relations(connection, schema)?,
            self.list_enums(connection, schema)?,
            self.list_sequences(connection, schema)?,
        ))
    }
}
