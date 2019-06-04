use crate::sql_database_migration_inferrer::wrap_as_step;
use crate::sql_migration_step::*;
use database_inspector::{Column, DatabaseSchema, Table};

const MIGRATION_TABLE_NAME: &str = "_Migration";

pub struct DatabaseSchemaDiffer<'a> {
    previous: &'a DatabaseSchema,
    next: &'a DatabaseSchema,
}

impl<'a> DatabaseSchemaDiffer<'a> {
    pub fn diff(previous: &DatabaseSchema, next: &DatabaseSchema) -> Vec<SqlMigrationStep> {
        let differ = DatabaseSchemaDiffer { previous, next };
        differ.diff_internal()
    }

    fn diff_internal(&self) -> Vec<SqlMigrationStep> {
        let mut result = Vec::new();
        result.append(&mut wrap_as_step(self.drop_tables(), |x| {
            SqlMigrationStep::DropTable(x)
        }));
        // let (create_tables, delayed_foreign_keys) = self.delay_foreign_key_creation(self.create_tables());
        // result.append(&mut wrap_as_step(create_tables, |x| SqlMigrationStep::CreateTable(x)));
        // result.append(&mut wrap_as_step(delayed_foreign_keys, |x| {
        //     SqlMigrationStep::AlterTable(x)
        // }));

        result.append(&mut wrap_as_step(self.create_tables(), |x| {
            SqlMigrationStep::CreateTable(x)
        }));
        result.append(&mut wrap_as_step(self.alter_tables(), |x| {
            SqlMigrationStep::AlterTable(x)
        }));
        result
    }

    fn create_tables(&self) -> Vec<CreateTable> {
        let mut result = Vec::new();
        for next_table in &self.next.tables {
            if !self.previous.has_table(&next_table.name) && next_table.name != MIGRATION_TABLE_NAME {
                let create = CreateTable {
                    name: next_table.name.clone(),
                    columns: Self::column_descriptions(&next_table.columns),
                    primary_columns: next_table.primary_key_columns.clone(),
                };
                result.push(create);
            }
        }
        result
    }

    // this function caters for the case that a table gets created that has a foreign key to a table that still needs to be created
    // Example: Table A has a reference to Table B and Table B has a reference to Table A.
    // We therefore split the creation of foreign key columns into separate steps when the referenced tables are not existing yet.
    // FIXME: This does not work with SQLite. A required column might get delayed. SQLite then fails with: "Cannot add a NOT NULL column with default value NULL"
    #[allow(unused)]
    fn delay_foreign_key_creation(&self, create_tables: Vec<CreateTable>) -> (Vec<CreateTable>, Vec<AlterTable>) {
        let mut alter_tables = Vec::new();
        let mut creates = create_tables;
        let table_names_that_get_created: Vec<String> = creates.iter().map(|t| t.name.clone()).collect();
        for create_table in creates.iter_mut() {
            let mut column_that_need_to_be_done_later_for_this_table = Vec::new();
            for column in &create_table.columns {
                if let Some(ref foreign_key) = column.foreign_key {
                    let references_non_existent_table = table_names_that_get_created.contains(&foreign_key.table);
                    let is_part_of_primary_key = create_table.primary_columns.contains(&column.name);
                    let is_relation_table = create_table.name.starts_with("_"); // todo: this is a very weak check. find a better one

                    if references_non_existent_table && !is_part_of_primary_key && !is_relation_table {
                        let change = column.clone();
                        column_that_need_to_be_done_later_for_this_table.push(change);
                    }
                }
            }
            // remove columns from the create that will be instead added later
            create_table
                .columns
                .retain(|c| !column_that_need_to_be_done_later_for_this_table.contains(&c));
            let changes = column_that_need_to_be_done_later_for_this_table
                .into_iter()
                .map(|c| TableChange::AddColumn(AddColumn { column: c }))
                .collect();

            let alter_table = AlterTable {
                table: create_table.name.clone(),
                changes: changes,
            };
            if !alter_table.changes.is_empty() {
                alter_tables.push(alter_table);
            }
        }
        (creates, alter_tables)
    }

    fn drop_tables(&self) -> Vec<DropTable> {
        let mut result = Vec::new();
        for previous_table in &self.previous.tables {
            if !self.next.has_table(&previous_table.name) && previous_table.name != MIGRATION_TABLE_NAME {
                let drop = DropTable {
                    name: previous_table.name.clone(),
                };
                result.push(drop);
            }
        }
        result
    }

    fn alter_tables(&self) -> Vec<AlterTable> {
        // TODO: this does not diff primary key columns yet
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

    pub fn column_descriptions(columns: &Vec<Column>) -> Vec<ColumnDescription> {
        columns.iter().map(Self::column_description).collect()
    }

    fn column_description(column: &Column) -> ColumnDescription {
        let fk = column.foreign_key.as_ref().map(|fk| ForeignKey {
            table: fk.table.clone(),
            column: fk.column.clone(),
        });
        ColumnDescription {
            name: column.name.clone(),
            tpe: Self::convert_column_type(column.tpe),
            required: column.is_required,
            foreign_key: fk,
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
