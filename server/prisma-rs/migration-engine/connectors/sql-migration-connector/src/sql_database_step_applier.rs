use crate::*;
use barrel::Migration as BarrelMigration;
use datamodel::ScalarType;
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
            SqlMigrationStep::CreateTable(CreateTable { name, columns }) => {
                migration.create_table(name, move |t| {
                    for column in columns.clone() {
                        let tpe = column_description_to_barrel_type(&column);
                        t.add_column(column.name, tpe);
                    }
                });
            }
            x => panic!(format!("{:?} not implemented yet here", x)),
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
    let tpe = match column_description.tpe {
        ScalarType::Boolean => barrel::types::boolean(),
        ScalarType::DateTime => barrel::types::date(),
        ScalarType::Decimal => unimplemented!(),
        ScalarType::Enum => barrel::types::varchar(255),
        ScalarType::Float => barrel::types::float(),
        ScalarType::Int => barrel::types::integer(),
        ScalarType::String => barrel::types::text(),
    };
    tpe.nullable(!column_description.required)
}
