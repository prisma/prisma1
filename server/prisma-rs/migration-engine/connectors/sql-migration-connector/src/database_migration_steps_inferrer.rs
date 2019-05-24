use crate::*;
use database_inspector::{
    relational::{
        RelationalIntrospectionConnector, RelationalIntrospectionResult, SchemaInfo as DatabaseSchema,
        TableInfo as Table,
    },
    IntrospectionConnector,
};
use datamodel::*;
use migration_connector::{steps::*, *};
use prisma_query::{error::Error as SqlError, transaction::Connection};

pub struct SqlDatabaseMigrationStepsInferrer {
    schema_name: String,
}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepsInferrer<SqlMigrationStep> for SqlDatabaseMigrationStepsInferrer {
    type DatabaseSchemaType = DatabaseSchema;

    fn infer(
        &self,
        previous: &Schema,
        next: &Schema,
        previous_database: &DatabaseSchema,
        next_database: &DatabaseSchema,
        steps: Vec<MigrationStep>,
    ) -> Vec<SqlMigrationStep> {
        //let current_database_schema = self.inspector.introspect(self.connection, &self.schema_name)?.schema;
        //let expected_database_schema = DatabaseSchemaCalculator::calculate(next).schema;
        let steps = DatabaseSchemaDiffer::diff(&previous_database, &next_database, &self.schema_name);
        let is_sqlite = true;
        if is_sqlite {
            self.fix_stupid_sqlite(steps, &previous_database, &next_database)
        } else {
            steps
        }
    }
}

impl SqlDatabaseMigrationStepsInferrer {
    fn fix_stupid_sqlite(
        &self,
        steps: Vec<SqlMigrationStep>,
        current_database_schema: &DatabaseSchema,
        next_database_schema: &DatabaseSchema,
    ) -> Vec<SqlMigrationStep> {
        let mut result = Vec::new();
        for step in steps {
            match step {
                SqlMigrationStep::AlterTable(ref alter_table) if self.needs_fix(&alter_table) => {
                    let current_table = current_database_schema.table(&alter_table.table).unwrap();
                    let next_table = next_database_schema.table(&alter_table.table).unwrap();
                    let mut altered_steps = self.fix(&alter_table, &current_table, &next_table, next_database_schema);
                    result.append(&mut altered_steps);
                }
                x => result.push(x),
            }
        }
        result
    }

    fn needs_fix(&self, alter_table: &AlterTable) -> bool {
        let change_that_does_not_work_on_sqlite = alter_table.changes.iter().find(|change| match change {
            TableChange::AddColumn(_) => false,
            TableChange::DropColumn(_) => true,
            TableChange::AlterColumn(_) => true,
        });
        change_that_does_not_work_on_sqlite.is_some()
    }

    fn fix(&self, _alter_table: &AlterTable, current: &Table, next: &Table, next_schema: &DatabaseSchema) -> Vec<SqlMigrationStep> {
        // based on 'Making Other Kinds Of Table Schema Changes' from https://www.sqlite.org/lang_altertable.html
        let name_of_temporary_table = format!("new_{}", next.name.clone());
        vec![
            SqlMigrationStep::RawSql("PRAGMA foreign_keys=OFF;".to_string()),
            // todo: start transaction now
            SqlMigrationStep::CreateTable(CreateTable {
                name: format!("new_{}", next.name.clone()),
                columns: DatabaseSchemaDiffer::column_descriptions(&next.columns, next, &next_schema.relations),
                primary_columns: match next.primary_key {
                    None => vec![],
                    Some(idx) => idx.columns.clone(),
                },
            }),
            // copy table contents
            {
                let current_columns: Vec<String> = current.columns.iter().map(|c| c.name.clone()).collect();
                let next_columns: Vec<String> = next.columns.iter().map(|c| c.name.clone()).collect();
                let intersection_columns: Vec<String> = current_columns
                    .into_iter()
                    .filter(|c| next_columns.contains(&c))
                    .collect();
                let columns_string = intersection_columns.join(",");
                let sql = format!(
                    "INSERT INTO {} ({}) SELECT {} from {}",
                    name_of_temporary_table,
                    columns_string,
                    columns_string,
                    next.name.clone()
                );
                SqlMigrationStep::RawSql(sql.to_string())
            },
            SqlMigrationStep::DropTable(DropTable {
                name: current.name.clone(),
            }),
            SqlMigrationStep::RenameTable {
                name: name_of_temporary_table,
                new_name: next.name.clone(),
            },
            // todo: recreate indexes + triggers
            SqlMigrationStep::RawSql(format!(r#"PRAGMA "{}".foreign_key_check;"#, self.schema_name)),
            // todo: commit transaction
            SqlMigrationStep::RawSql("PRAGMA foreign_keys=ON;".to_string()),
        ]
    }
}

pub fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<SqlMigrationStep>
where
    F: FnMut(T) -> SqlMigrationStep,
{
    steps.into_iter().map(|x| wrap_fn(x)).collect()
}
