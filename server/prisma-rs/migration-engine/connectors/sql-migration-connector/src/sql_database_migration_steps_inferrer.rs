use crate::database_schema_calculator::DatabaseSchemaCalculator;
use crate::database_schema_differ::DatabaseSchemaDiffer;
use crate::sql_migration_step::*;
use database_inspector::{DatabaseInspector, DatabaseSchema, Table};
use datamodel::*;
use migration_connector::steps::*;
use migration_connector::*;

pub struct SqlDatabaseMigrationStepsInferrer {
    pub inspector: Box<DatabaseInspector>,
    pub schema_name: String,
}

impl DatabaseMigrationStepsInferrer<SqlMigration> for SqlDatabaseMigrationStepsInferrer {
    fn infer(&self, _previous: &Datamodel, next: &Datamodel, _steps: &Vec<MigrationStep>) -> SqlMigration {
        let current_database_schema = self.inspector.introspect(&self.schema_name);
        let expected_database_schema = DatabaseSchemaCalculator::calculate(next);
        let steps = self.infer_database_migration_steps_and_fix(&current_database_schema, &expected_database_schema);
        let rollback = self.infer_database_migration_steps_and_fix(&expected_database_schema, &current_database_schema);
        SqlMigration {
            steps: steps,
            rollback: rollback,
        }
    }
}

impl SqlDatabaseMigrationStepsInferrer {
    fn infer_database_migration_steps_and_fix(
        &self,
        from: &DatabaseSchema,
        to: &DatabaseSchema,
    ) -> Vec<SqlMigrationStep> {
        let steps = DatabaseSchemaDiffer::diff(&from, &to);
        let is_sqlite = true;
        if is_sqlite {
            self.fix_stupid_sqlite(steps, &from, &to)
        } else {
            steps
        }
    }

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
                    let mut altered_steps = self.fix(&alter_table, &current_table, &next_table);
                    result.append(&mut altered_steps);
                }
                x => result.push(x),
            }
        }
        result
    }

    fn needs_fix(&self, alter_table: &AlterTable) -> bool {
        let change_that_does_not_work_on_sqlite = alter_table.changes.iter().find(|change| match change {
            TableChange::AddColumn(add_column) => {
                // sqlite does not allow adding not null columns without a default value even if the table is empty
                // hence we just use our normal migration process
                // https://laracasts.com/discuss/channels/general-discussion/migrations-sqlite-general-error-1-cannot-add-a-not-null-column-with-default-value-null
                add_column.column.required
            }
            TableChange::DropColumn(_) => true,
            TableChange::AlterColumn(_) => true,
        });
        change_that_does_not_work_on_sqlite.is_some()
    }

    fn fix(&self, _alter_table: &AlterTable, current: &Table, next: &Table) -> Vec<SqlMigrationStep> {
        // based on 'Making Other Kinds Of Table Schema Changes' from https://www.sqlite.org/lang_altertable.html
        let name_of_temporary_table = format!("new_{}", next.name.clone());
        vec![
            SqlMigrationStep::RawSql {
                raw: "PRAGMA foreign_keys=OFF;".to_string(),
            },
            // todo: start transaction now
            SqlMigrationStep::CreateTable(CreateTable {
                name: format!("new_{}", next.name.clone()),
                columns: DatabaseSchemaDiffer::column_descriptions(&next.columns),
                primary_columns: next.primary_key_columns.clone(),
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
                SqlMigrationStep::RawSql { raw: sql.to_string() }
            },
            SqlMigrationStep::DropTable(DropTable {
                name: current.name.clone(),
            }),
            SqlMigrationStep::RenameTable {
                name: name_of_temporary_table,
                new_name: next.name.clone(),
            },
            // todo: recreate indexes + triggers
            SqlMigrationStep::RawSql {
                raw: format!(r#"PRAGMA "{}".foreign_key_check;"#, self.schema_name),
            },
            // todo: commit transaction
            SqlMigrationStep::RawSql {
                raw: "PRAGMA foreign_keys=ON;".to_string(),
            },
        ]
    }
}

pub fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<SqlMigrationStep>
where
    F: FnMut(T) -> SqlMigrationStep,
{
    steps.into_iter().map(|x| wrap_fn(x)).collect()
}
