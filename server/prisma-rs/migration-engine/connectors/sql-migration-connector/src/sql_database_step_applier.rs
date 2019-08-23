use crate::*;
use database_introspection::*;
use datamodel::Value;
use migration_connector::*;
use std::sync::Arc;

pub struct SqlDatabaseStepApplier {
    pub sql_family: SqlFamily,
    pub schema_name: String,
    pub conn: Arc<dyn MigrationDatabase + Send + Sync + 'static>,
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

impl SqlDatabaseStepApplier {
    fn apply_next_step(&self, steps: &Vec<SqlMigrationStep>, index: usize) -> SqlResult<bool> {
        let has_this_one = steps.get(index).is_some();
        if !has_this_one {
            return Ok(false);
        }

        let step = &steps[index];
        let sql_string = render_raw_sql(&step, self.sql_family, &self.schema_name);
        debug!("{}", sql_string);

        let result = self.conn.query_raw(&self.schema_name, &sql_string, &[]);

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
    let schema_name = schema_name.to_string();

    match step {
        SqlMigrationStep::CreateTable(CreateTable { table }) => {
            let cloned_columns = table.columns.clone();
            let primary_columns = table.primary_key_columns();
            let mut lines = Vec::new();
            for column in cloned_columns.clone() {
                let col_sql = render_column(sql_family, schema_name.to_string(), &table, &column, false);
                lines.push(col_sql);
            }
            if primary_columns.len() > 0 {
                let column_names: Vec<String> = primary_columns
                    .clone()
                    .into_iter()
                    .map(|col| quote(&col, sql_family))
                    .collect();
                lines.push(format!("PRIMARY KEY ({})", column_names.join(",")))
            }
            format!(
                "CREATE TABLE {}.{}({})\n{};",
                quote(&schema_name, sql_family),
                quote(&table.name, sql_family),
                lines.join(","),
                create_table_suffix(sql_family),
            )
        }
        SqlMigrationStep::DropTable(DropTable { name }) => format!(
            "DROP TABLE {}.{};",
            quote(&schema_name, sql_family),
            quote(name, sql_family)
        ),
        SqlMigrationStep::DropTables(DropTables { names }) => {
            let fully_qualified_names: Vec<String> = names
                .iter()
                .map(|name| format!("{}.{}", quote(&schema_name, sql_family), quote(name, sql_family)))
                .collect();
            format!("DROP TABLE {};", fully_qualified_names.join(","))
        }
        SqlMigrationStep::RenameTable { name, new_name } => {
            let new_name = match sql_family {
                SqlFamily::Sqlite => format!("{}", quote(new_name, sql_family)),
                _ => format!("{}.{}", quote(&schema_name, sql_family), quote(new_name, sql_family)),
            };
            format!(
                "ALTER TABLE {}.{} RENAME TO {};",
                quote(&schema_name, sql_family),
                quote(name, sql_family),
                new_name
            )
        }
        SqlMigrationStep::AlterTable(AlterTable { table, changes }) => {
            let mut lines = Vec::new();
            for change in changes.clone() {
                match change {
                    TableChange::AddColumn(AddColumn { column }) => {
                        let col_sql = render_column(sql_family, schema_name.to_string(), &table, &column, true);
                        lines.push(format!("ADD COLUMN {}", col_sql));
                    }
                    TableChange::DropColumn(DropColumn { name }) => {
                        // TODO: this does not work on MySQL for columns with foreign keys. Here the FK must be dropped first by name.
                        let name = quote(&name, sql_family);
                        lines.push(format!("DROP COLUMN {}", name));
                    }
                    TableChange::AlterColumn(AlterColumn { name, column }) => {
                        let name = quote(&name, sql_family);
                        lines.push(format!("DROP COLUMN {}", name));
                        let col_sql = render_column(sql_family, schema_name.to_string(), &table, &column, true);
                        lines.push(format!("ADD COLUMN {}", col_sql));
                    }
                }
            }
            format!(
                "ALTER TABLE {}.{} {};",
                quote(&schema_name, sql_family),
                quote(&table.name, sql_family),
                lines.join(",")
            )
        }
        SqlMigrationStep::CreateIndex(CreateIndex {
            table,
            index,
        }) => {
            let Index { name, columns, tpe } = index;
            let index_type = match tpe {
                IndexType::Unique => "UNIQUE",
                IndexType::Normal => "",
            };
            let index_name = match sql_family {
                SqlFamily::Sqlite => format!("{}.{}", quote(&schema_name, sql_family), quote(&name, sql_family)),
                _ => quote(&name, sql_family),
            };
            let table_reference = match sql_family {
                SqlFamily::Sqlite => quote(&table, sql_family),
                _ => format!("{}.{}", quote(&schema_name, sql_family), quote(&table, sql_family)),
            };
            let columns: Vec<String> = columns.iter().map(|c| quote(c, sql_family)).collect();
            format!(
                "CREATE {} INDEX {} ON {}({})",
                index_type,
                index_name,
                table_reference,
                columns.join(",")
            )
        }
        SqlMigrationStep::DropIndex(DropIndex { table, name }) => match sql_family {
            SqlFamily::Mysql => format!(
                "DROP INDEX {} ON {}.{}",
                quote(&name, sql_family),
                quote(&schema_name, sql_family),
                quote(&table, sql_family)
            ),
            SqlFamily::Postgres | SqlFamily::Sqlite => format!(
                "DROP INDEX {}.{}",
                quote(&schema_name, sql_family),
                quote(&name, sql_family)
            ),
        },
        SqlMigrationStep::RawSql { raw } => raw.to_string(),
    }
}

fn quote(name: &str, sql_family: SqlFamily) -> String {
    match sql_family {
        SqlFamily::Sqlite => format!("\"{}\"", name),
        SqlFamily::Postgres => format!("\"{}\"", name),
        SqlFamily::Mysql => format!("`{}`", name),
    }
}

fn create_table_suffix(sql_family: SqlFamily) -> String {
    match sql_family {
        SqlFamily::Sqlite => "".to_string(),
        SqlFamily::Postgres => "".to_string(),
        SqlFamily::Mysql => "DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci".to_string(),
    }
}

fn render_column(
    sql_family: SqlFamily,
    schema_name: String,
    table: &Table,
    column: &Column,
    add_fk_prefix: bool,
) -> String {
    let column_name = quote(&column.name, sql_family);
    let tpe_str = render_column_type(sql_family, &column.tpe);
    // TODO: bring back when the query planning for writes is done
    let nullability_str = if column.is_required() && !table.is_part_of_foreign_key(&column.name) {
        "NOT NULL"
    } else {
        ""
    };
    let default_str = match &column.default {
        Some(value) => {
            let default = match column.tpe.family {
                ColumnTypeFamily::String | ColumnTypeFamily::DateTime => {
                    // TODO: find a better solution for this amazing hack. the default value must not be a String
                    if value.starts_with("'") {
                        format!("DEFAULT {}", value)
                    } else {
                        format!("DEFAULT '{}'", value)
                    }
                }
                _ => format!("DEFAULT {}", value),
            };
            // we use the default value right now only to smoothen migrations. So we only use it when absolutely needed.
            if column.is_required() {
                default
            } else {
                "".to_string()
            }
        }
        None => "".to_string(),
    };
    let foreign_key = table.foreign_key_for_column(&column.name);
    let references_str = match (sql_family, foreign_key) {
        (SqlFamily::Postgres, Some(fk)) => format!(
            "REFERENCES \"{}\".\"{}\"(\"{}\") {}",
            schema_name,
            fk.referenced_table,
            fk.referenced_columns.first().unwrap(),
            render_on_delete(&fk.on_delete_action)
        ),
        (SqlFamily::Mysql, Some(fk)) => format!(
            "REFERENCES `{}`.`{}`(`{}`) {}",
            schema_name,
            fk.referenced_table,
            fk.referenced_columns.first().unwrap(),
            render_on_delete(&fk.on_delete_action)
        ),
        (SqlFamily::Sqlite, Some(fk)) => format!(
            "REFERENCES \"{}\"({}) {}",
            fk.referenced_table,
            fk.referenced_columns.first().unwrap(),
            render_on_delete(&fk.on_delete_action)
        ),
        (_, None) => "".to_string(),
    };
    match (sql_family, foreign_key) {
        (SqlFamily::Mysql, Some(_)) => {
            let add = if add_fk_prefix { "ADD" } else { "" };
            let fk_line = format!("{} FOREIGN KEY ({}) {}", add, column_name, references_str);
            format!(
                "{} {} {} {},{}",
                column_name, tpe_str, nullability_str, default_str, fk_line
            )
        }
        _ => format!(
            "{} {} {} {} {}",
            column_name, tpe_str, nullability_str, default_str, references_str
        ),
    }
}

fn render_on_delete(on_delete: &ForeignKeyAction) -> &'static str {
    match on_delete {
        ForeignKeyAction::NoAction => "",
        ForeignKeyAction::SetNull => "ON DELETE SET NULL",
        ForeignKeyAction::Cascade => "ON DELETE CASCADE",
        ForeignKeyAction::SetDefault => "ON DELETE SET DEFAULT",
        ForeignKeyAction::Restrict => "ON DELETE RESTRICT",
    }
}

// TODO: this returns None for expressions
fn render_value(value: &Value) -> Option<String> {
    match value {
        Value::Boolean(x) => Some(if *x { "true".to_string() } else { "false".to_string() }),
        Value::Int(x) => Some(format!("{}", x)),
        Value::Float(x) => Some(format!("{}", x)),
        Value::Decimal(x) => Some(format!("{}", x)),
        Value::String(x) => Some(format!("'{}'", x)),

        Value::DateTime(x) => {
            let mut raw = format!("{}", x); // this will produce a String 1970-01-01 00:00:00 UTC
            raw.truncate(raw.len() - 4); // strip the UTC suffix
            Some(format!("'{}'", raw)) // add quotes
        }
        Value::ConstantLiteral(x) => Some(format!("'{}'", x)), // this represents enum values
        _ => None,
    }
}

// TODO: this must become database specific akin to our TypeMappers in Scala
fn render_column_type(sql_family: SqlFamily, t: &ColumnType) -> String {
    match sql_family {
        SqlFamily::Sqlite => render_column_type_sqlite(t),
        SqlFamily::Postgres => render_column_type_postgres(t),
        SqlFamily::Mysql => render_column_type_mysql(t),
    }
}

fn render_column_type_sqlite(t: &ColumnType) -> String {
    match &t.family {
        ColumnTypeFamily::Boolean => format!("BOOLEAN"),
        ColumnTypeFamily::DateTime => format!("DATE"),
        ColumnTypeFamily::Float => format!("REAL"),
        ColumnTypeFamily::Int => format!("INTEGER"),
        ColumnTypeFamily::String => format!("TEXT"),
        x => unimplemented!("{:?} not handled yet", x),
    }
}

fn render_column_type_postgres(t: &ColumnType) -> String {
    match &t.family {
        ColumnTypeFamily::Boolean => format!("boolean"),
        ColumnTypeFamily::DateTime => format!("timestamp(3)"),
        ColumnTypeFamily::Float => format!("Decimal(65,30)"),
        ColumnTypeFamily::Int => format!("integer"),
        ColumnTypeFamily::String => format!("text"),
        x => unimplemented!("{:?} not handled yet", x),
    }
}

fn render_column_type_mysql(t: &ColumnType) -> String {
    match &t.family {
        ColumnTypeFamily::Boolean => format!("boolean"),
        ColumnTypeFamily::DateTime => format!("datetime(3)"),
        ColumnTypeFamily::Float => format!("Decimal(65,30)"),
        ColumnTypeFamily::Int => format!("int"),
        // we use varchar right now as mediumtext doesn't allow default values
        // a bigger length would not allow to use such a column as primary key
        ColumnTypeFamily::String => format!("varchar(191)"),
        x => unimplemented!("{:?} not handled yet", x),
    }
}
