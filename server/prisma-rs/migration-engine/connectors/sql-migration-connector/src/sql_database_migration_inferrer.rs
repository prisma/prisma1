use crate::database_schema_calculator::DatabaseSchemaCalculator;
use crate::database_schema_differ::{DatabaseSchemaDiff, DatabaseSchemaDiffer};
use crate::*;
use database_introspection::*;
use datamodel::*;
use migration_connector::steps::*;
use migration_connector::*;
use std::sync::Arc;

pub struct SqlDatabaseMigrationInferrer {
    pub sql_family: SqlFamily,
    pub introspector: Arc<dyn IntrospectionConnector + Send + Sync + 'static>,
    pub schema_name: String,
}

impl DatabaseMigrationInferrer<SqlMigration> for SqlDatabaseMigrationInferrer {
    fn infer(
        &self,
        _previous: &Datamodel,
        next: &Datamodel,
        _steps: &Vec<MigrationStep>,
    ) -> ConnectorResult<SqlMigration> {
        let current_database_schema: DatabaseSchema = self.introspect(&self.schema_name)?;
        let expected_database_schema = DatabaseSchemaCalculator::calculate(next)?;
        infer(
            &current_database_schema,
            &expected_database_schema,
            &self.schema_name,
            self.sql_family,
        )
    }
}

impl SqlDatabaseMigrationInferrer {
    fn introspect(&self, schema: &str) -> SqlResult<DatabaseSchema> {
        Ok(self.introspector.introspect(&schema)?)
    }
}

fn infer(
    current_database_schema: &DatabaseSchema,
    expected_database_schema: &DatabaseSchema,
    schema_name: &str,
    sql_family: SqlFamily,
) -> ConnectorResult<SqlMigration> {
    let steps = infer_database_migration_steps_and_fix(
        &current_database_schema,
        &expected_database_schema,
        &schema_name,
        sql_family,
    )?;
    let rollback = infer_database_migration_steps_and_fix(
        &expected_database_schema,
        &current_database_schema,
        &schema_name,
        sql_family,
    )?;
    Ok(SqlMigration {
        before: current_database_schema.clone(),
        after: expected_database_schema.clone(),
        steps,
        rollback,
    })
}

fn infer_database_migration_steps_and_fix(
    from: &DatabaseSchema,
    to: &DatabaseSchema,
    schema_name: &str,
    sql_family: SqlFamily,
) -> SqlResult<Vec<SqlMigrationStep>> {
    let diff: DatabaseSchemaDiff = DatabaseSchemaDiffer::diff(&from, &to);
    let is_sqlite = sql_family == SqlFamily::Sqlite;

    if is_sqlite {
        fix_stupid_sqlite(diff, &from, &to, &schema_name)
    } else {
        let steps = delay_foreign_key_creation(diff);
        fix_id_column_type_change(&from, &to, schema_name, steps)
    }
}

fn fix_id_column_type_change(
    from: &DatabaseSchema,
    to: &DatabaseSchema,
    _schema_name: &str,
    steps: Vec<SqlMigrationStep>,
) -> SqlResult<Vec<SqlMigrationStep>> {
    let has_id_type_change = steps
        .iter()
        .find(|step| match step {
            SqlMigrationStep::AlterTable(alter_table) => {
                if let Ok(current_table) = from.table(&alter_table.table.name) {
                    let change_to_id_column = alter_table.changes.iter().find(|c| match c {
                        TableChange::AlterColumn(alter_column) => {
                            let current_column = current_table.column_bang(&alter_column.name);
                            let current_column_type = &current_column.tpe;
                            let has_type_changed = current_column_type.family != alter_column.column.tpe.family; // TODO: take into account raw type
                            let is_part_of_pk = current_table
                                .primary_key
                                .clone()
                                .map(|pk| pk.columns)
                                .unwrap_or(vec![])
                                .contains(&alter_column.name);
                            is_part_of_pk && has_type_changed
                        }
                        _ => false,
                    });
                    change_to_id_column.is_some()
                } else {
                    false
                }
            }
            _ => false,
        })
        .is_some();

    // TODO: There's probably a much more graceful way to handle this. But this would also involve a lot of data loss probably. Let's tackle that after P Day
    if has_id_type_change {
        let mut radical_steps = Vec::new();
        let tables_to_drop: Vec<String> = from
            .tables
            .iter()
            .filter(|t| t.name != "_Migration")
            .map(|t| t.name.clone())
            .collect();
        radical_steps.push(SqlMigrationStep::DropTables(DropTables { names: tables_to_drop }));
        let diff_from_empty: DatabaseSchemaDiff = DatabaseSchemaDiffer::diff(&DatabaseSchema::empty(), &to);
        let mut steps_from_empty = delay_foreign_key_creation(diff_from_empty);
        radical_steps.append(&mut steps_from_empty);

        Ok(radical_steps)
    } else {
        Ok(steps)
    }
}

// this function caters for the case that a table gets created that has a foreign key to a table that still needs to be created
// Example: Table A has a reference to Table B and Table B has a reference to Table A.
// We therefore split the creation of foreign key columns into separate steps when the referenced tables are not existing yet.
// FIXME: This does not work with SQLite. A required column might get delayed. SQLite then fails with: "Cannot add a NOT NULL column with default value NULL"
fn delay_foreign_key_creation(mut diff: DatabaseSchemaDiff) -> Vec<SqlMigrationStep> {
    let names_of_tables_that_get_created: Vec<String> =
        diff.create_tables.iter().map(|t| t.table.name.clone()).collect();
    let mut extra_alter_tables = Vec::new();

    // This mutates the CreateTables in place to remove the foreign key creation. Instead the foreign key creation is moved into separate AlterTable statements.
    for create_table in diff.create_tables.iter_mut() {
        let mut column_that_need_to_be_done_later_for_this_table = Vec::new();
        for column in &create_table.table.columns {
            if let Some(ref foreign_key) = create_table.table.foreign_key_for_column(&column.name) {
                let references_non_existent_table =
                    names_of_tables_that_get_created.contains(&foreign_key.referenced_table);
                let is_part_of_primary_key = create_table.table.is_part_of_primary_key(&column.name);
                let is_relation_table = create_table.table.name.starts_with("_"); // todo: this is a very weak check. find a better one

                if references_non_existent_table && !is_part_of_primary_key && !is_relation_table {
                    let change = column.clone();
                    column_that_need_to_be_done_later_for_this_table.push(change);
                }
            }
        }
        // remove columns from the create that will be instead added later
        create_table
            .table
            .columns
            .retain(|c| !column_that_need_to_be_done_later_for_this_table.contains(&c));
        let changes = column_that_need_to_be_done_later_for_this_table
            .into_iter()
            .map(|c| TableChange::AddColumn(AddColumn { column: c.clone() }))
            .collect();

        let alter_table = AlterTable {
            table: create_table.table.clone(),
            changes,
        };
        if !alter_table.changes.is_empty() {
            extra_alter_tables.push(alter_table);
        }
    }
    diff.alter_tables.append(&mut extra_alter_tables);
    diff.into_steps()
}

fn fix_stupid_sqlite(
    diff: DatabaseSchemaDiff,
    current_database_schema: &DatabaseSchema,
    next_database_schema: &DatabaseSchema,
    schema_name: &str,
) -> SqlResult<Vec<SqlMigrationStep>> {
    let steps = diff.into_steps();
    let mut result = Vec::new();
    let mut fixed_tables = Vec::new();
    for step in steps {
        match step {
            SqlMigrationStep::AlterTable(ref alter_table) if needs_fix(&alter_table) => {
                let current_table = current_database_schema.table(&alter_table.table.name)?;
                let next_table = next_database_schema.table(&alter_table.table.name)?;
                let mut altered_steps = fix(&alter_table, &current_table, &next_table, &schema_name);
                result.append(&mut altered_steps);
                fixed_tables.push(current_table.name.clone());
            }
            SqlMigrationStep::CreateIndex(ref create_index) if fixed_tables.contains(&create_index.table) => {
                // The fixed alter table step will already create the index
            }
            x => result.push(x),
        }
    }
    Ok(result)
}

fn needs_fix(alter_table: &AlterTable) -> bool {
    let change_that_does_not_work_on_sqlite = alter_table.changes.iter().find(|change| match change {
        TableChange::AddColumn(add_column) => {
            // sqlite does not allow adding not null columns without a default value even if the table is empty
            // hence we just use our normal migration process
            // https://laracasts.com/discuss/channels/general-discussion/migrations-sqlite-general-error-1-cannot-add-a-not-null-column-with-default-value-null
            add_column.column.arity == ColumnArity::Required
        }
        TableChange::DropColumn(_) => true,
        TableChange::AlterColumn(_) => true,
    });
    change_that_does_not_work_on_sqlite.is_some()
}

fn fix(_alter_table: &AlterTable, current: &Table, next: &Table, schema_name: &str) -> Vec<SqlMigrationStep> {
    // based on 'Making Other Kinds Of Table Schema Changes' from https://www.sqlite.org/lang_altertable.html
    let name_of_temporary_table = format!("new_{}", next.name.clone());
    let mut temporary_table = next.clone();
    temporary_table.name = name_of_temporary_table.clone();

    let mut result = Vec::new();

    result.push(SqlMigrationStep::RawSql {
        raw: "PRAGMA foreign_keys=OFF;".to_string(),
    });
    // todo: start transaction now. Unclear if we really want to do that.
    result.push(SqlMigrationStep::CreateTable(CreateTable { table: temporary_table }));
    result.push(
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
    );
    result.push(SqlMigrationStep::DropTable(DropTable {
        name: current.name.clone(),
    }));
    result.push(SqlMigrationStep::RenameTable {
        name: name_of_temporary_table,
        new_name: next.name.clone(),
    });
    result.append(
        &mut next
            .indices
            .iter()
            .map(|index| {
                SqlMigrationStep::CreateIndex(CreateIndex {
                    table: next.name.clone(),
                    index: index.clone(),
                })
            })
            .collect(),
    );
    // todo: recreate triggers
    result.push(SqlMigrationStep::RawSql {
        raw: format!(r#"PRAGMA "{}".foreign_key_check;"#, schema_name),
    });
    // todo: commit transaction
    result.push(SqlMigrationStep::RawSql {
        raw: "PRAGMA foreign_keys=ON;".to_string(),
    });

    result
}

pub fn wrap_as_step<T, F>(steps: Vec<T>, mut wrap_fn: F) -> Vec<SqlMigrationStep>
where
    F: FnMut(T) -> SqlMigrationStep,
{
    steps.into_iter().map(|x| wrap_fn(x)).collect()
}
