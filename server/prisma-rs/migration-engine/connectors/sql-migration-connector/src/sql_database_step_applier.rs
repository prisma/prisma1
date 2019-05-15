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
impl DatabaseMigrationStepApplier<SqlMigrationStep> for SqlDatabaseStepApplier {
    fn apply(&self, step: SqlMigrationStep) {
        let mut migration = BarrelMigration::new().schema(self.schema_name.clone());

        match dbg!(step) {
            SqlMigrationStep::CreateTable(CreateTable {
                name,
                columns,
                primary_columns,
            }) => {
                migration.create_table(name, move |t| {
                    for column in columns.clone() {
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
            }
            SqlMigrationStep::DropTable(DropTable { name }) => {
                migration.drop_table(name);
            }
            x => {
                //panic!(format!("{:?} not implemented yet here", x)),
                println!("{:?} not implemented yet here", x);
            }
        };
        let sql_string = dbg!(self.make_sql_string(migration));
        dbg!(self.connection.execute(&sql_string, NO_PARAMS)).unwrap();
    }
}

impl SqlDatabaseStepApplier {
    fn make_sql_string(&self, migration: BarrelMigration) -> String {
        // TODO: this should pattern match on the connector type once we have this information available
        migration.make::<barrel::backend::Sqlite>()
    }
}

fn column_description_to_barrel_type(column_description: &ColumnDescription) -> barrel::types::Type {
    // TODO: add foreign keys for non integer types once this is available in barrel
    let tpe = match &column_description.foreign_key {
        Some(fk) => barrel::types::foreign(string_to_static_str(format!("{}({})", fk.table, fk.column))),
        None => match column_description.tpe {
            ColumnType::Boolean => barrel::types::boolean(),
            ColumnType::DateTime => barrel::types::date(),
            ColumnType::Float => barrel::types::float(),
            ColumnType::Int => barrel::types::integer(),
            ColumnType::String => barrel::types::text(),
        },
    };
    tpe.nullable(!column_description.required)
}

fn string_to_static_str(s: String) -> &'static str {
    Box::leak(s.into_boxed_str())
}
