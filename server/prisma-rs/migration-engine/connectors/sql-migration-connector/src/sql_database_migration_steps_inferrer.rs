use crate::database_schema_calculator::DatabaseSchemaCalculator;
use crate::database_schema_differ::DatabaseSchemaDiffer;
use crate::sql_migration_step::*;
use database_inspector::{DatabaseInspector, DatabaseSchema, Table};
use datamodel::*;
use migration_connector::steps::*;
use migration_connector::*;
use prisma_query::*;

pub struct SqlDatabaseMigrationStepsInferrer {
    pub inspector: Box<DatabaseInspector>,
    pub schema_name: String,
}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepsInferrer<SqlMigrationStep> for SqlDatabaseMigrationStepsInferrer {
    fn infer(&self, previous: &Schema, next: &Schema, steps: Vec<MigrationStep>) -> Vec<SqlMigrationStep> {
        let current_database_schema = self.inspector.introspect(&self.schema_name);
        let expected_database_schema = DatabaseSchemaCalculator::calculate(next);
        let steps = DatabaseSchemaDiffer::diff(&current_database_schema, &expected_database_schema);
        let is_sqlite = true;
        if is_sqlite {
            fix_stupid_sqlite(steps, &current_database_schema, &expected_database_schema)
        } else {
            steps
        }
    }
}

fn fix_stupid_sqlite(
    steps: Vec<SqlMigrationStep>,
    current_database_schema: &DatabaseSchema,
    next_database_schema: &DatabaseSchema,
) -> Vec<SqlMigrationStep> {
    let mut result = Vec::new();
    for step in steps {
        match step {
            SqlMigrationStep::AlterTable(ref alter_table) if needs_fix(&alter_table) => {
                let current_table = current_database_schema.table(&alter_table.table).unwrap();
                let next_table = next_database_schema.table(&alter_table.table).unwrap();
                let mut altered_steps = fix(&alter_table, &current_table, &next_table);
                result.append(&mut altered_steps);
            }
            x => result.push(x),
        }
    }
    result
}

fn needs_fix(alter_table: &AlterTable) -> bool {
    let change_that_does_not_work_on_sqlite = alter_table.changes.iter().find(|change| match change {
        TableChange::AddColumn(_) => false,
        TableChange::DropColumn(_) => true,
        TableChange::AlterColumn(_) => true,
    });
    change_that_does_not_work_on_sqlite.is_some()
}

fn fix(alter_table: &AlterTable, current: &Table, next: &Table) -> Vec<SqlMigrationStep> {
    // based on 'Making Other Kinds Of Table Schema Changes' from https://www.sqlite.org/lang_altertable.html
    let name_of_temporary_table = format!("new_{}", next.name.clone());
    vec![
        SqlMigrationStep::RawSql("PRAGMA foreign_keys=OFF;".to_string()),
        // todo: start transaction now
        SqlMigrationStep::CreateTable(CreateTable {
            name: format!("new_{}", next.name.clone()),
            columns: DatabaseSchemaDiffer::column_descriptions(&next.columns),
            primary_columns: next.primary_key_columns.clone(),
        }),
        // todo: copy table contents
        SqlMigrationStep::DropTable(DropTable {
            name: current.name.clone(),
        }),
        SqlMigrationStep::RenameTable {
            name: name_of_temporary_table,
            new_name: next.name.clone(),
        },
        // todo: recreate indexes + triggers
        // SqlMigrationStep::RawSql("PRAGMA schema.foreign_key_check;".to_string()), // todo add right schema
        // todo: commit transaction
        SqlMigrationStep::RawSql("PRAGMA foreign_keys=ON;".to_string()),
    ]
}

pub fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<SqlMigrationStep>
where
    F: FnMut(T) -> SqlMigrationStep,
{
    steps.into_iter().map(|x| wrap_fn(x)).collect()
}
