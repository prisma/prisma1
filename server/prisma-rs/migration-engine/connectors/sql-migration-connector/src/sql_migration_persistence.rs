#[allow(unused, dead_code)]
use barrel::types;
use chrono::*;
use migration_connector::*;
use prisma_query::{ast::*, visitor::*};
use rusqlite::{Connection, Row, NO_PARAMS};
use serde_json;

pub struct SqlMigrationPersistence {
    connection: Connection,
    schema_name: String,
}

impl SqlMigrationPersistence {
    pub fn new(conn: Connection, schema_name: String) -> SqlMigrationPersistence {
        SqlMigrationPersistence {
            connection: conn,
            schema_name,
        }
    }

    pub fn init(&self) {
        let mut m = barrel::Migration::new().schema(self.schema_name.clone());
        m.create_table_if_not_exists(TABLE_NAME, |t| {
            t.add_column(REVISION_COLUMN, types::primary());
            t.add_column(NAME_COLUMN, types::text());
            t.add_column(DATAMODEL_COLUMN, types::text());
            t.add_column(STATUS_COLUMN, types::text());
            t.add_column(APPLIED_COLUMN, types::integer());
            t.add_column(ROLLED_BACK_COLUMN, types::integer());
            t.add_column(DATAMODEL_STEPS_COLUMN, types::text());
            t.add_column(DATABASE_MIGRATION_COLUMN, types::text());
            t.add_column(ERRORS_COLUMN, types::text());
            t.add_column(STARTED_AT_COLUMN, types::date());
            t.add_column(FINISHED_AT_COLUMN, types::date().nullable(true));
        });

        let sql_str = m.make::<barrel::backend::Sqlite>();

        dbg!(self.connection.execute(&sql_str, NO_PARAMS).unwrap());
    }
}

#[allow(unused, dead_code)]
impl MigrationPersistence for SqlMigrationPersistence {
    fn last(&self) -> Option<Migration> {
        let conditions = STATUS_COLUMN.equals("Success");
        let query = Select::from_table(TABLE_NAME)
            .so_that(conditions)
            .order_by(REVISION_COLUMN.descend());
        let (sql_str, params) = Sqlite::build(query);

        self.connection
            .query_row(&sql_str, params, |row| Ok(parse_row(row)))
            .ok()
    }

    fn load_all(&self) -> Vec<Migration> {
        let query = Select::from_table(TABLE_NAME);
        let (sql_str, params) = dbg!(Sqlite::build(query));

        let mut stmt = self.connection.prepare_cached(&sql_str).unwrap();
        let mut rows = stmt.query(params).unwrap();
        let mut result = Vec::new();

        while let Some(row) = rows.next().unwrap() {
            result.push(parse_row(&row));
        }

        result
    }

    fn by_name(&self, name: &str) -> Option<Migration> {
        let conditions = NAME_COLUMN.equals(name);
        let query = Select::from_table(TABLE_NAME)
            .so_that(conditions)
            .order_by(REVISION_COLUMN.descend());
        let (sql_str, params) = Sqlite::build(query);

        self.connection
            .query_row(&sql_str, params, |row| Ok(parse_row(row)))
            .ok()
    }

    fn create(&self, migration: Migration) -> Migration {
        let finished_at_value = match migration.finished_at {
            Some(x) => x.timestamp_millis().into(),
            None => ParameterizedValue::Null,
        };
        let mut cloned = migration.clone();
        let model_steps_json = serde_json::to_string(&migration.datamodel_steps).unwrap();
        let database_migration_json = migration.database_migration;
        let errors_json = serde_json::to_string(&migration.errors).unwrap();
        let serialized_datamodel = datamodel::render(&migration.datamodel).unwrap();

        let query = Insert::single_into(TABLE_NAME)
            .value(NAME_COLUMN, migration.name)
            .value(DATAMODEL_COLUMN, serialized_datamodel)
            .value(STATUS_COLUMN, migration.status.code())
            .value(APPLIED_COLUMN, migration.applied)
            .value(ROLLED_BACK_COLUMN, migration.rolled_back)
            .value(DATAMODEL_STEPS_COLUMN, model_steps_json)
            .value(DATABASE_MIGRATION_COLUMN, database_migration_json)
            .value(ERRORS_COLUMN, errors_json)
            .value(
                STARTED_AT_COLUMN,
                ParameterizedValue::Integer(migration.started_at.timestamp_millis()),
            )
            .value(FINISHED_AT_COLUMN, finished_at_value);

        let (sql_str, params) = dbg!(Sqlite::build(query));

        let result = dbg!(self.connection.execute(&sql_str, params));

        cloned.revision = self.connection.last_insert_rowid() as usize;
        cloned
    }

    fn update(&self, params: &MigrationUpdateParams) {
        let finished_at_value = match params.finished_at {
            Some(x) => x.timestamp_millis().into(),
            None => ParameterizedValue::Null,
        };
        let errors_json = serde_json::to_string(&params.errors).unwrap();
        let query = Update::table(TABLE_NAME)
            .set(STATUS_COLUMN, params.status.code())
            .set(APPLIED_COLUMN, params.applied)
            .set(ROLLED_BACK_COLUMN, params.rolled_back)
            .set(ERRORS_COLUMN, errors_json)
            .set(FINISHED_AT_COLUMN, finished_at_value)
            .so_that(
                NAME_COLUMN
                    .equals(params.name.clone())
                    .and(REVISION_COLUMN.equals(params.revision)),
            );

        let (sql_str, params) = dbg!(Sqlite::build(query));

        let result = dbg!(self.connection.execute(&sql_str, params));
    }
}

fn timestamp_to_datetime(timestamp: i64) -> DateTime<Utc> {
    let nsecs = ((timestamp % 1000) * 1_000_000) as u32;
    let secs = (timestamp / 1000) as i64;
    let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
    let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

    datetime
}

fn parse_row(row: &Row) -> Migration {
    let revision: u32 = row.get(REVISION_COLUMN).unwrap();
    let applied: u32 = row.get(APPLIED_COLUMN).unwrap();
    let rolled_back: u32 = row.get(ROLLED_BACK_COLUMN).unwrap();
    let errors_json: String = row.get(ERRORS_COLUMN).unwrap();
    let errors: Vec<String> = serde_json::from_str(&errors_json).unwrap();
    let finished_at: Option<i64> = row.get(FINISHED_AT_COLUMN).unwrap();
    let database_migration_string: String = row.get(DATABASE_MIGRATION_COLUMN).unwrap();
    let database_migration_json = serde_json::from_str(&database_migration_string).unwrap();
    let datamodel_steps_json: String = row.get(DATAMODEL_STEPS_COLUMN).unwrap();
    let datamodel_string: String = row.get(DATAMODEL_COLUMN).unwrap();

    let datamodel_steps = serde_json::from_str(&datamodel_steps_json).unwrap();
    let datamodel = datamodel::parse(&datamodel_string).unwrap();
    Migration {
        name: row.get(NAME_COLUMN).unwrap(),
        revision: revision as usize,
        datamodel: datamodel,
        status: MigrationStatus::from_str(row.get(STATUS_COLUMN).unwrap()),
        applied: applied as usize,
        rolled_back: rolled_back as usize,
        datamodel_steps: datamodel_steps,
        database_migration: database_migration_json,
        errors: errors,
        started_at: timestamp_to_datetime(row.get(STARTED_AT_COLUMN).unwrap()),
        finished_at: finished_at.map(timestamp_to_datetime),
    }
}

static TABLE_NAME: &str = "_Migration";
static NAME_COLUMN: &str = "name";
static REVISION_COLUMN: &str = "revision";
static DATAMODEL_COLUMN: &str = "datamodel";
static STATUS_COLUMN: &str = "status";
static APPLIED_COLUMN: &str = "applied";
static ROLLED_BACK_COLUMN: &str = "rolled_back";
static DATAMODEL_STEPS_COLUMN: &str = "datamodel_steps";
static DATABASE_MIGRATION_COLUMN: &str = "database_migration";
static ERRORS_COLUMN: &str = "errors";
static STARTED_AT_COLUMN: &str = "started_at";
static FINISHED_AT_COLUMN: &str = "finished_at";
