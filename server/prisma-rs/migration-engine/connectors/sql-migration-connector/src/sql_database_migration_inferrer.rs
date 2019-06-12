use crate::database_schema_calculator::DatabaseSchemaCalculator;
use crate::database_schema_differ::DatabaseSchemaDiffer;
use crate::*;
use database_inspector::{DatabaseInspector, DatabaseSchema, Table};
use datamodel::*;
use migration_connector::steps::*;
use migration_connector::*;
use std::sync::Arc;

pub struct SqlDatabaseMigrationInferrer {
    pub inspector: Arc<DatabaseInspector>,
    pub schema_name: String,
}

impl DatabaseMigrationInferrer<SqlMigration> for SqlDatabaseMigrationInferrer {
    fn infer(&self, _previous: &Datamodel, next: &Datamodel, _steps: &Vec<MigrationStep>) -> SqlMigration {
        let current_database_schema = self.inspector.introspect(&self.schema_name);
        let expected_database_schema = DatabaseSchemaCalculator::calculate(next);
        infer(&current_database_schema, &expected_database_schema, &self.schema_name)
    }
}

pub struct VirtualSqlDatabaseMigrationInferrer {
    pub schema_name: String,
}
impl DatabaseMigrationInferrer<SqlMigration> for VirtualSqlDatabaseMigrationInferrer {
    fn infer(&self, previous: &Datamodel, next: &Datamodel, _steps: &Vec<MigrationStep>) -> SqlMigration {
        let current_database_schema = DatabaseSchemaCalculator::calculate(previous);
        let expected_database_schema = DatabaseSchemaCalculator::calculate(next);
        infer(&current_database_schema, &expected_database_schema, &self.schema_name)
    }
}

fn infer(current: &DatabaseSchema, next: &DatabaseSchema, schema_name: &str) -> SqlMigration {
    let steps = infer_database_migration_steps_and_fix(&current, &next, &schema_name);
    let rollback = infer_database_migration_steps_and_fix(&next, &current, &schema_name);
    SqlMigration {
        steps: steps,
        rollback: rollback,
    }
}

fn infer_database_migration_steps_and_fix(
    from: &DatabaseSchema,
    to: &DatabaseSchema,
    schema_name: &str,
) -> Vec<SqlMigrationStep> {
    let steps = DatabaseSchemaDiffer::diff(&from, &to);
    let is_sqlite = true;
    if is_sqlite {
        fix_stupid_sqlite(steps, &from, &to, &schema_name)
    } else {
        steps
    }
}

fn fix_stupid_sqlite(
    steps: Vec<SqlMigrationStep>,
    current_database_schema: &DatabaseSchema,
    next_database_schema: &DatabaseSchema,
    schema_name: &str,
) -> Vec<SqlMigrationStep> {
    let mut result = Vec::new();
    for step in steps {
        match step {
            SqlMigrationStep::AlterTable(ref alter_table) if needs_fix(&alter_table) => {
                let current_table = current_database_schema.table(&alter_table.table).unwrap();
                let next_table = next_database_schema.table(&alter_table.table).unwrap();
                let mut altered_steps = fix(&alter_table, &current_table, &next_table, &schema_name);
                result.append(&mut altered_steps);
            }
            x => result.push(x),
        }
    }
    result
}

fn needs_fix(alter_table: &AlterTable) -> bool {
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

fn fix(_alter_table: &AlterTable, current: &Table, next: &Table, schema_name: &str) -> Vec<SqlMigrationStep> {
    // based on 'Making Other Kinds Of Table Schema Changes' from https://www.sqlite.org/lang_altertable.html
    let name_of_temporary_table = format!("new_{}", next.name.clone());
    vec![
        SqlMigrationStep::RawSql {
            raw: "PRAGMA foreign_keys=OFF;".to_string(),
        },
        // todo: start transaction now
        SqlMigrationStep::CreateTable(CreateTable {
            name: name_of_temporary_table.clone(),
            columns: DatabaseSchemaDiffer::column_descriptions(&next.columns),
            primary_columns: next.primary_key_columns.clone(),
        }),
        // copy table contents; Here we have to handle escpaing ourselves.
        {
            let current_columns: Vec<String> = current.columns.iter().map(|c| c.name.clone()).collect();
            let next_columns: Vec<String> = next.columns.iter().map(|c| c.name.clone()).collect();
            let intersection_columns: Vec<String> = current_columns
                .into_iter()
                .filter(|c| next_columns.contains(&c))
                .collect();
            let columns_string = intersection_columns
                .iter()
                .map(|c| format!("\"{}\"", c))
                .collect::<Vec<String>>()
                .join(",");
            let sql = format!(
                "INSERT INTO \"{}\" ({}) SELECT {} from \"{}\"",
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
            raw: format!(r#"PRAGMA "{}".foreign_key_check;"#, schema_name),
        },
        // todo: commit transaction
        SqlMigrationStep::RawSql {
            raw: "PRAGMA foreign_keys=ON;".to_string(),
        },
    ]
}

pub fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<SqlMigrationStep>
where
    F: FnMut(T) -> SqlMigrationStep,
{
    steps.into_iter().map(|x| wrap_fn(x)).collect()
}
