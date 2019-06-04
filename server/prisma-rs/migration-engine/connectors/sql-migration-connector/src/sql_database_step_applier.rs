use crate::*;
use barrel::Migration as BarrelMigration;
use migration_connector::*;
use rusqlite::{Connection, NO_PARAMS};

pub struct SqlDatabaseStepApplier {
    connection: Connection,
    schema_name: String,
}

impl SqlDatabaseStepApplier {
    pub fn new(connection: Connection, schema_name: String) -> Self {
        SqlDatabaseStepApplier {
            connection,
            schema_name,
        }
    }
}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepApplier<SqlMigration> for SqlDatabaseStepApplier {
    fn apply_step(&self, database_migration: &SqlMigration, index: usize) -> bool {
        self.apply_next_step(&database_migration.steps, index)
    }

    fn unapply_step(&self, database_migration: &SqlMigration, index: usize) -> bool {
        self.apply_next_step(&database_migration.rollback, index)
    }

    fn render_steps_pretty(&self, database_migration: &SqlMigration) -> serde_json::Value {
        let jsons = database_migration
            .steps
            .iter()
            .map(|step| {
                let cloned = step.clone();
                let mut json_value = serde_json::to_value(&step).unwrap();
                let json_object = json_value.as_object_mut().unwrap();
                json_object.insert(
                    "raw".to_string(),
                    serde_json::Value::String(self.render_raw_sql(&cloned)),
                );
                json_value
            })
            .collect();
        serde_json::Value::Array(jsons)
    }

    fn render_steps_internal(&self, database_migration: &SqlMigration) -> serde_json::Value {
        serde_json::to_value(&database_migration).unwrap()
    }
}

impl SqlDatabaseStepApplier {
    fn apply_next_step(&self, steps: &Vec<SqlMigrationStep>, index: usize) -> bool {
        let has_this_one = steps.get(index).is_some();
        if !has_this_one {
            return false;
        }

        let step = &steps[index];
        let sql_string = self.render_raw_sql(&step);
        dbg!(&sql_string);
        let result = self.connection.execute(&sql_string, NO_PARAMS);
        // TODO: this does not evaluate the results of the PRAGMA foreign_key_check
        match dbg!(result) {
            Ok(_) => {}
            Err(rusqlite::Error::ExecuteReturnedResults) => {} // renames return results and crash the driver ..
            e @ Err(_) => {
                e.unwrap();
                {}
            }
        }

        let has_more = steps.get(index + 1).is_some();
        has_more
    }

    fn render_raw_sql(&self, step: &SqlMigrationStep) -> String {
        let mut migration = BarrelMigration::new().schema(self.schema_name.clone());

        match step {
            SqlMigrationStep::CreateTable(CreateTable {
                name,
                columns,
                primary_columns,
            }) => {
                let cloned_columns = columns.clone();
                let primary_columns = primary_columns.clone();
                migration.create_table(name.to_string(), move |t| {
                    for column in cloned_columns.clone() {
                        let tpe = column_description_to_barrel_type(&column);
                        t.add_column(column.name, tpe);
                    }
                    if primary_columns.len() > 0 {
                        let column_names: Vec<String> = primary_columns
                            .clone()
                            .into_iter()
                            .map(|col| format!("\"{}\"", col))
                            .collect();
                        t.inject_custom(format!("PRIMARY KEY ({})", column_names.join(",")));
                    }
                });
                self.make_sql_string(migration)
            }
            SqlMigrationStep::DropTable(DropTable { name }) => {
                migration.drop_table(name.to_string());
                self.make_sql_string(migration)
            }
            SqlMigrationStep::RenameTable { name, new_name } => {
                migration.rename_table(name.to_string(), new_name.to_string());
                self.make_sql_string(migration)
            }
            SqlMigrationStep::AlterTable(AlterTable { table, changes }) => {
                let changes = changes.clone();
                migration.change_table(table.to_string(), move |t| {
                    for change in changes.clone() {
                        match change {
                            TableChange::AddColumn(AddColumn { column }) => {
                                let tpe = column_description_to_barrel_type(&column);
                                t.add_column(column.name, tpe);
                            }
                            TableChange::DropColumn(DropColumn { name }) => t.drop_column(name),
                            TableChange::AlterColumn(AlterColumn { name, column }) => {
                                t.drop_column(name);
                                let tpe = column_description_to_barrel_type(&column);
                                t.add_column(column.name, tpe);
                            }
                        }
                    }
                });
                self.make_sql_string(migration)
            }
            SqlMigrationStep::RawSql { raw } => raw.to_string(),
        }
    }

    fn make_sql_string(&self, migration: BarrelMigration) -> String {
        // TODO: this should pattern match on the connector type once we have this information available
        migration.make::<barrel::backend::Sqlite>()
    }
}

fn column_description_to_barrel_type(column_description: &ColumnDescription) -> barrel::types::Type {
    // TODO: add foreign keys for non integer types once this is available in barrel
    let tpe = match &column_description.foreign_key {
        Some(fk) => {
            let tpe_str = render_column_type(column_description.tpe);
            let complete = dbg!(format!("{} REFERENCES {}({})", tpe_str, fk.table, fk.column));
            barrel::types::custom(string_to_static_str(complete))
        }
        None => {
            let tpe_str = render_column_type(column_description.tpe);
            barrel::types::custom(string_to_static_str(tpe_str))
        }
    };
    tpe.nullable(!column_description.required)
}

// TODO: this must become database specific akin to our TypeMappers in Scala
fn render_column_type(t: ColumnType) -> String {
    match t {
        ColumnType::Boolean => format!("BOOLEAN"),
        ColumnType::DateTime => format!("DATE"),
        ColumnType::Float => format!("REAL"),
        ColumnType::Int => format!("INTEGER"),
        ColumnType::String => format!("TEXT"),
    }
}

fn string_to_static_str(s: String) -> &'static str {
    Box::leak(s.into_boxed_str())
}
