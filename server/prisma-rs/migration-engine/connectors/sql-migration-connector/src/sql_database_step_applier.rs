use crate::*;
use barrel::Migration as BarrelMigration;
use migration_connector::*;
use prisma_query::Connectional;
use std::sync::Arc;
use datamodel::Value;

pub struct SqlDatabaseStepApplier {
    pub sql_family: SqlFamily,
    pub schema_name: String,
    pub conn: Arc<Connectional>,
}

#[allow(unused, dead_code)]
impl DatabaseMigrationStepApplier<SqlMigration> for SqlDatabaseStepApplier {
    fn apply_step(&self, database_migration: &SqlMigration, index: usize) -> ConnectorResult<bool> {
        Ok(self.apply_next_step(&database_migration.steps, index)?)
    }

    fn unapply_step(&self, database_migration: &SqlMigration, index: usize) -> ConnectorResult<bool> {
        Ok(self.apply_next_step(&database_migration.rollback, index)?)
    }

    fn render_steps_pretty(&self, database_migration: &SqlMigration) -> ConnectorResult<serde_json::Value> {
        Ok(render_steps_pretty(
            &database_migration,
            self.sql_family,
            &self.schema_name,
        )?)
    }
}

pub struct VirtualSqlDatabaseStepApplier {
    pub sql_family: SqlFamily,
    pub schema_name: String,
}
impl DatabaseMigrationStepApplier<SqlMigration> for VirtualSqlDatabaseStepApplier {
    fn apply_step(&self, _database_migration: &SqlMigration, _index: usize) -> ConnectorResult<bool> {
        unimplemented!("Not allowed on a VirtualSqlDatabaseStepApplier")
    }

    fn unapply_step(&self, _database_migration: &SqlMigration, _index: usize) -> ConnectorResult<bool> {
        unimplemented!("Not allowed on a VirtualSqlDatabaseStepApplier")
    }

    fn render_steps_pretty(&self, database_migration: &SqlMigration) -> ConnectorResult<serde_json::Value> {
        Ok(render_steps_pretty(
            &database_migration,
            self.sql_family,
            &self.schema_name,
        )?)
    }
}

impl SqlDatabaseStepApplier {
    fn apply_next_step(&self, steps: &Vec<SqlMigrationStep>, index: usize) -> SqlResult<bool> {
        let has_this_one = steps.get(index).is_some();
        if !has_this_one {
            return Ok(false);
        }

        let step = &steps[index];
        let sql_string = render_raw_sql(&step, self.sql_family, &self.schema_name);
        dbg!(&sql_string);
        let result = self.conn.query_on_raw_connection(&self.schema_name, &sql_string, &[]);

        // TODO: this does not evaluate the results of SQLites PRAGMA foreign_key_check
        result?;

        let has_more = steps.get(index + 1).is_some();
        Ok(has_more)
    }
}

fn render_steps_pretty(
    database_migration: &SqlMigration,
    sql_family: SqlFamily,
    schema_name: &str,
) -> ConnectorResult<serde_json::Value> {
    let jsons = database_migration
        .steps
        .iter()
        .map(|step| {
            let cloned = step.clone();
            let mut json_value = serde_json::to_value(&step).unwrap();
            let json_object = json_value.as_object_mut().unwrap();
            json_object.insert(
                "raw".to_string(),
                serde_json::Value::String(render_raw_sql(&cloned, sql_family, schema_name)),
            );
            json_value
        })
        .collect();
    Ok(serde_json::Value::Array(jsons))
}

fn render_raw_sql(step: &SqlMigrationStep, sql_family: SqlFamily, schema_name: &str) -> String {
    let mut migration = BarrelMigration::new().schema(schema_name.clone());

    let schema_name = schema_name.to_string();

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
                    let tpe = column_description_to_barrel_type(sql_family, schema_name.to_string(), &column);
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
            make_sql_string(migration, sql_family)
        }
        SqlMigrationStep::DropTable(DropTable { name }) => {
            migration.drop_table(name.to_string());
            make_sql_string(migration, sql_family)
        }
        SqlMigrationStep::DropTables(DropTables { names }) => {
            let fully_qualified_names: Vec<String> = names
                .iter()
                .map(|name| format!("\"{}\".\"{}\"", schema_name, name))
                .collect();
            format!("DROP TABLE {};", fully_qualified_names.join(","))
        }
        SqlMigrationStep::RenameTable { name, new_name } => {
            migration.rename_table(name.to_string(), new_name.to_string());
            make_sql_string(migration, sql_family)
        }
        SqlMigrationStep::AlterTable(AlterTable { table, changes }) => {
            let changes = changes.clone();
            migration.change_table(table.to_string(), move |t| {
                for change in changes.clone() {
                    match change {
                        TableChange::AddColumn(AddColumn { column }) => {
                            let tpe = column_description_to_barrel_type(sql_family, schema_name.to_string(), &column);
                            t.add_column(column.name, tpe);
                        }
                        TableChange::DropColumn(DropColumn { name }) => t.drop_column(name),
                        TableChange::AlterColumn(AlterColumn { name, column }) => {
                            t.drop_column(name);
                            let tpe = column_description_to_barrel_type(sql_family, schema_name.to_string(), &column);
                            t.add_column(column.name, tpe);
                        }
                    }
                }
            });
            make_sql_string(migration, sql_family)
        }
        SqlMigrationStep::RawSql { raw } => raw.to_string(),
    }
}

fn make_sql_string(migration: BarrelMigration, sql_family: SqlFamily) -> String {
    // TODO: this should pattern match on the connector type once we have this information available
    match sql_family {
        SqlFamily::Sqlite => migration.make::<barrel::backend::Sqlite>(),
        SqlFamily::Postgres => migration.make::<barrel::backend::Pg>(),
        SqlFamily::Mysql => migration.make::<barrel::backend::MySql>(),
    }
}

fn column_description_to_barrel_type(
    sql_family: SqlFamily,
    schema_name: String,
    column_description: &ColumnDescription,
) -> barrel::types::Type {
    // TODO: add foreign keys for non integer types once this is available in barrel
    let tpe_str = render_column_type(sql_family, column_description.tpe);
    let tpe_str_with_default = match &column_description.default {
        Some(value) => format!("{} DEFAULT {}", tpe_str.clone(), render_value(value)),
        None => tpe_str.clone(),
    };
    let tpe = match (sql_family, &column_description.foreign_key) {
        (SqlFamily::Postgres, Some(fk)) => {
            let complete = dbg!(format!(
                "{} REFERENCES \"{}\".\"{}\"({})",
                tpe_str, schema_name, fk.table, fk.column
            ));
            barrel::types::custom(string_to_static_str(complete))
        }
        (_, Some(fk)) => {
            let complete = dbg!(format!("{} REFERENCES {}({})", tpe_str, fk.table, fk.column));
            barrel::types::custom(string_to_static_str(complete))
        }
        (_, None) => barrel::types::custom(string_to_static_str(tpe_str_with_default)),
    };
    tpe.nullable(!column_description.required)
}

fn render_value(value: &Value) -> String {
    match value {
        Value::Boolean(x) => if *x { "true".to_string() } else { "false".to_string() },
        Value::Int(x) => format!("{}", x),
        Value::Float(x) => format!("{}", x),
        Value::Decimal(x) => format!("{}", x),
        Value::String(x) => format!("'{}'", x),
        
        Value::DateTime(x) => {
            let mut raw = format!("{}", x); // this will produce a String 1970-01-01 00:00:00 UTC
            raw.truncate(raw.len() - 4); // strip the UTC suffix
            format!("'{}'", raw) // add quotes
        },
        Value::ConstantLiteral(x) => format!("'{}'", x), // this represents enum values
        _ => unimplemented!(),
    }
}

// TODO: this must become database specific akin to our TypeMappers in Scala
fn render_column_type(sql_family: SqlFamily, t: ColumnType) -> String {
    match sql_family {
        SqlFamily::Sqlite => render_column_type_sqlite(t),
        SqlFamily::Postgres => render_column_type_postgres(t),
        _ => unimplemented!(),
    }
}

fn render_column_type_sqlite(t: ColumnType) -> String {
    match t {
        ColumnType::Boolean => format!("BOOLEAN"),
        ColumnType::DateTime => format!("DATE"),
        ColumnType::Float => format!("REAL"),
        ColumnType::Int => format!("INTEGER"),
        ColumnType::String => format!("TEXT"),
    }
}

fn render_column_type_postgres(t: ColumnType) -> String {
    match t {
        ColumnType::Boolean => format!("boolean"),
        ColumnType::DateTime => format!("timestamp(3)"),
        ColumnType::Float => format!("Decimal(65,30)"),
        ColumnType::Int => format!("integer"),
        ColumnType::String => format!("text"),
    }
}

fn string_to_static_str(s: String) -> &'static str {
    Box::leak(s.into_boxed_str())
}
