use crate::sql_database_migration_steps_inferrer::wrap_as_step;
use crate::sql_migration_step::*;
use database_inspector::{Column, DatabaseSchema, Table};

pub struct DatabaseSchemaDiffer {
    previous: DatabaseSchema,
    next: DatabaseSchema,
}

impl DatabaseSchemaDiffer {
    pub fn diff(previous: DatabaseSchema, next: DatabaseSchema) -> Vec<SqlMigrationStep> {
        let differ = DatabaseSchemaDiffer { previous, next };
        differ.diff_internal()
    }

    fn diff_internal(&self) -> Vec<SqlMigrationStep> {
        let mut result = Vec::new();
        result.append(&mut wrap_as_step(self.create_tables(), |x| {
            SqlMigrationStep::CreateTable(x)
        }));
        result.append(&mut wrap_as_step(self.drop_tables(), |x| {
            SqlMigrationStep::DropTable(x)
        }));
        result.append(&mut wrap_as_step(self.alter_tables(), |x| {
            SqlMigrationStep::AlterTable(x)
        }));
        result
    }

    fn create_tables(&self) -> Vec<CreateTable> {
        let mut result = Vec::new();
        for next_table in &self.next.tables {
            if !self.previous.has_table(&next_table.name) {
                let primary_columns = next_table
                    .indexes
                    .iter()
                    .find(|i| i.unique)
                    .map(|i| i.columns.clone())
                    .unwrap_or(Vec::new());

                let create = CreateTable {
                    name: next_table.name.clone(),
                    columns: Self::column_descriptions(&next_table.columns),
                    primary_columns: primary_columns,
                };
                result.push(create);
            }
        }
        result
    }

    fn drop_tables(&self) -> Vec<DropTable> {
        let mut result = Vec::new();
        for previous_table in &self.previous.tables {
            if !self.next.has_table(&previous_table.name) && previous_table.name != "_Migration" {
                let drop = DropTable {
                    name: previous_table.name.clone(),
                };
                result.push(drop);
            }
        }
        result
    }

    fn alter_tables(&self) -> Vec<AlterTable> {
        let mut result = Vec::new();
        for previous_table in &self.previous.tables {
            if let Some(next_table) = self.next.table(&previous_table.name) {
                let mut changes = Vec::new();
                changes.append(&mut Self::drop_columns(&previous_table, &next_table));
                changes.append(&mut Self::add_columns(&previous_table, &next_table));
                changes.append(&mut Self::alter_columns(&previous_table, &next_table));

                if !changes.is_empty() {
                    let update = AlterTable {
                        table: previous_table.name.clone(),
                        changes: changes,
                    };
                    result.push(update);
                }
            }
        }
        result
    }

    fn drop_columns(previous: &Table, next: &Table) -> Vec<TableChange> {
        let mut result = Vec::new();
        for previous_column in &previous.columns {
            if !next.has_column(&previous_column.name) {
                let change = DropColumn {
                    name: previous_column.name.clone(),
                };
                result.push(TableChange::DropColumn(change));
            }
        }
        result
    }

    fn add_columns(previous: &Table, next: &Table) -> Vec<TableChange> {
        let mut result = Vec::new();
        for next_column in &next.columns {
            if !previous.has_column(&next_column.name) {
                let change = AddColumn {
                    column: Self::column_description(next_column),
                };
                result.push(TableChange::AddColumn(change));
            }
        }
        result
    }

    fn alter_columns(previous: &Table, next: &Table) -> Vec<TableChange> {
        let mut result = Vec::new();
        for next_column in &next.columns {
            if let Some(previous_column) = previous.column(&next_column.name) {
                if previous_column != next_column {
                    let change = AlterColumn {
                        name: previous_column.name.clone(),
                        column: Self::column_description(next_column),
                    };
                    result.push(TableChange::AlterColumn(change));
                }
            }
        }
        result
    }

    fn column_descriptions(columns: &Vec<Column>) -> Vec<ColumnDescription> {
        columns.iter().map(Self::column_description).collect()
    }

    fn column_description(column: &Column) -> ColumnDescription {
        ColumnDescription {
            name: column.name.clone(),
            tpe: Self::convert_column_type(column.tpe),
            required: column.is_required,
        }
    }

    fn convert_column_type(inspector_type: database_inspector::ColumnType) -> ColumnType {
        match inspector_type {
            database_inspector::ColumnType::Boolean => ColumnType::Boolean,
            database_inspector::ColumnType::Int => ColumnType::Int,
            database_inspector::ColumnType::Float => ColumnType::Float,
            database_inspector::ColumnType::String => ColumnType::String,
            database_inspector::ColumnType::DateTime => ColumnType::DateTime,
        }
    }
}
