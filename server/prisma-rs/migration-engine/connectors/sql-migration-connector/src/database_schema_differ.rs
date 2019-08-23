use crate::*;
use database_introspection::*;

const MIGRATION_TABLE_NAME: &str = "_Migration";

#[derive(Debug)]
pub struct DatabaseSchemaDiffer<'a> {
    previous: &'a DatabaseSchema,
    next: &'a DatabaseSchema,
}

#[derive(Debug, Clone)]
pub struct DatabaseSchemaDiff {
    pub drop_tables: Vec<DropTable>,
    pub create_tables: Vec<CreateTable>,
    pub alter_tables: Vec<AlterTable>,
    pub create_indexes: Vec<CreateIndex>,
    pub drop_indexes: Vec<DropIndex>,
}

impl DatabaseSchemaDiff {
    pub fn into_steps(self) -> Vec<SqlMigrationStep> {
        let mut steps = Vec::new();
        steps.append(&mut wrap_as_step(self.drop_indexes, |x| SqlMigrationStep::DropIndex(x)));
        steps.append(&mut wrap_as_step(self.drop_tables, |x| SqlMigrationStep::DropTable(x)));
        steps.append(&mut wrap_as_step(self.create_tables, |x| {
            SqlMigrationStep::CreateTable(x)
        }));
        steps.append(&mut wrap_as_step(self.alter_tables, |x| {
            SqlMigrationStep::AlterTable(x)
        }));
        steps.append(&mut wrap_as_step(self.create_indexes, |x| {
            SqlMigrationStep::CreateIndex(x)
        }));
        steps
    }
}

impl<'a> DatabaseSchemaDiffer<'a> {
    pub fn diff(previous: &DatabaseSchema, next: &DatabaseSchema) -> DatabaseSchemaDiff {
        let differ = DatabaseSchemaDiffer { previous, next };
        differ.diff_internal()
    }

    fn diff_internal(&self) -> DatabaseSchemaDiff {
        DatabaseSchemaDiff {
            drop_tables: self.drop_tables(),
            create_tables: self.create_tables(),
            alter_tables: self.alter_tables(),
            create_indexes: self.create_indexes(),
            drop_indexes: self.drop_indexes(),
        }
    }

    fn create_tables(&self) -> Vec<CreateTable> {
        let mut result = Vec::new();
        for next_table in &self.next.tables {
            if !self.previous.has_table(&next_table.name) && next_table.name != MIGRATION_TABLE_NAME {
                let create = CreateTable {
                    table: next_table.clone(),
                };
                result.push(create);
            }
        }
        result
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
            if let Ok(next_table) = self.next.table(&previous_table.name) {
                let mut changes = Vec::new();
                changes.append(&mut Self::drop_columns(&previous_table, &next_table));
                changes.append(&mut Self::add_columns(&previous_table, &next_table));
                changes.append(&mut Self::alter_columns(&previous_table, &next_table));

                if !changes.is_empty() {
                    let update = AlterTable {
                        table: next_table.clone(),
                        changes,
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
                    column: next_column.clone(),
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
                let previous_fk = previous.foreign_key_for_column(&previous.name);
                let next_fk = next.foreign_key_for_column(&next.name);
                if previous_column.differs_in_something_except_default(next_column) || previous_fk != next_fk {
                    let change = AlterColumn {
                        name: previous_column.name.clone(),
                        column: next_column.clone(),
                    };
                    result.push(TableChange::AlterColumn(change));
                }
            }
        }
        result
    }

    fn create_indexes(&self) -> Vec<CreateIndex> {
        let mut result = Vec::new();
        for next_table in &self.next.tables {
            for index in &next_table.indices {
                // TODO: must diff index settings
                let previous_index_opt = self
                    .previous
                    .table(&next_table.name)
                    .ok()
                    .and_then(|t| t.indices.iter().find(|i| i.name == index.name));
                if let None = previous_index_opt {
                    let create = CreateIndex {
                        table: next_table.name.clone(),
                        index: index.clone(),
                    };
                    result.push(create);
                }
            }
        }
        result
    }

    fn drop_indexes(&self) -> Vec<DropIndex> {
        let mut result = Vec::new();
        for previous_table in &self.previous.tables {
            for index in &previous_table.indices {
                // TODO: must diff index settings
                let next_index_opt = self
                    .next
                    .table(&previous_table.name)
                    .ok()
                    .and_then(|t| t.indices.iter().find(|i| i.name == index.name));
                if let None = next_index_opt {
                    let drop = DropIndex {
                        table: previous_table.name.clone(),
                        name: index.name.clone(),
                    };
                    result.push(drop);
                }
            }
        }
        result
    }
}
