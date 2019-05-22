use crate::relational::*;
use crate::*;
use crate::{SqlError, Transaction};

pub trait SpecializedRelationalIntrospectionConnector {
    fn database_type(&self) -> &str;
    fn list_schemas(&self, transaction: &mut Transaction) -> Result<Vec<String>, SqlError>;
    fn query_tables(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<String>, SqlError>;
    fn query_relations(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<TableRelationInfo>, SqlError>;
    fn query_columns(
        &self,
        transaction: &mut Transaction,
        schema: &str,
        table: &str,
    ) -> Result<Vec<ColumnInfo>, SqlError>;
    fn query_column_comment(
        &self,
        transaction: &mut Transaction,
        schema: &str,
        table: &str,
        column: &str,
    ) -> Result<Option<String>, SqlError>;
    fn query_indices(
        &self,
        transaction: &mut Transaction,
        schema: &str,
        table: &str,
    ) -> Result<Vec<InternalIndexIntrospectionResult>, SqlError>;
    fn query_enums(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<EnumInfo>, SqlError>;
    fn query_sequences(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<SequenceInfo>, SqlError>;
    fn create_introspection_result(
        &self,
        models: &Vec<TableInfo>,
        relations: &Vec<TableRelationInfo>,
        enums: &Vec<EnumInfo>,
        sequences: &Vec<SequenceInfo>,
    ) -> RelationalIntrospectionResult;
}

pub struct RelationalIntrospectionConnector {
    specialized: Box<SpecializedRelationalIntrospectionConnector>,
}

impl RelationalIntrospectionConnector {
    pub fn new(specialized: Box<SpecializedRelationalIntrospectionConnector>) -> RelationalIntrospectionConnector {
        RelationalIntrospectionConnector { specialized }
    }

    fn list_models(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<TableInfo>, SqlError> {
        let mut tables: Vec<TableInfo> = vec![];
        let all_tables = self.specialized.query_tables(transaction, schema)?;

        for table_name in &all_tables {
            let mut columns = self.specialized.query_columns(transaction, schema, table_name)?;

            for column in &mut columns {
                column.comment =
                    self.specialized
                        .query_column_comment(transaction, schema, table_name, &column.name)?;
            }

            let all_indices = self.specialized.query_indices(transaction, schema, table_name)?;
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
                inidices: secondary_inidices,
                primary_key: primary_key,
            })
        }

        Ok(tables)
    }

    fn list_relations(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<TableRelationInfo>, SqlError> {
        self.specialized.query_relations(transaction, schema)
    }

    fn list_enums(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<EnumInfo>, SqlError> {
        self.specialized.query_enums(transaction, schema)
    }

    fn list_sequences(&self, transaction: &mut Transaction, schema: &str) -> Result<Vec<SequenceInfo>, SqlError> {
        self.specialized.query_sequences(transaction, schema)
    }
}

impl IntrospectionConnector<RelationalIntrospectionResult> for RelationalIntrospectionConnector {
    fn database_type(&self) -> &str {
        self.specialized.database_type()
    }

    fn list_schemas(&self, transaction: &mut Transaction) -> Result<Vec<String>, SqlError> {
        self.specialized.list_schemas(transaction)
    }

    fn introspect(
        &self,
        transaction: &mut Transaction,
        schema: &str,
    ) -> Result<RelationalIntrospectionResult, SqlError> {
        Ok(self.specialized.create_introspection_result(
            &self.list_models(transaction, schema)?,
            &self.list_relations(transaction, schema)?,
            &self.list_enums(transaction, schema)?,
            &self.list_sequences(transaction, schema)?,
        ))
    }
}
