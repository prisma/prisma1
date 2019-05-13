#[allow(unused, dead_code)]
use chrono::*;
use datamodel::Schema;
use migration_connector::*;
use prisma_query::{ast::*, visitor::*};
use rusqlite::{Connection, Row};
use serde_json;

pub struct SqlMigrationPersistence {
    connection: Connection,
}

impl SqlMigrationPersistence {
    pub fn new(conn: Connection) -> SqlMigrationPersistence {
        SqlMigrationPersistence { connection: conn }
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

        let result = self.connection.query_row(&sql_str, params, parse_row);
        result.ok()
    }

    fn load_all(&self) -> Vec<Migration> {
        let query = Select::from_table(TABLE_NAME);
        let (sql_str, params) = dbg!(Sqlite::build(query));

        let mut stmt = self.connection.prepare_cached(&sql_str).unwrap();
        let mut rows = stmt.query(params).unwrap();
        let mut result = Vec::new();

        while let Some(row) = rows.next() {
            result.push(parse_row(&row.unwrap()));
        }

        result
    }

    fn create(&self, migration: Migration) -> Migration {
        let finished_at_value = match migration.finished_at {
            Some(x) => x.timestamp_millis().into(),
            None => ParameterizedValue::Null,
        };
        let mut cloned = migration.clone();
        // let status_value = serde_json::to_string(&migration.status).unwrap();
        let model_steps_json = serde_json::to_string(&migration.datamodel_steps).unwrap();
        let database_steps_json = migration.database_steps;
        let errors_json = serde_json::to_string(&migration.errors).unwrap();

        let query = Insert::single_into(TABLE_NAME)
            .value(NAME_COLUMN, migration.name)
            .value(DATAMODEL_COLUMN, "".to_string()) // todo: serialize datamodel
            .value(STATUS_COLUMN, migration.status.code())
            .value(APPLIED_COLUMN, migration.applied)
            .value(ROLLED_BACK_COLUMN, migration.rolled_back)
            .value(DATAMODEL_STEPS_COLUMN, model_steps_json)
            .value(DATABASE_STEPS_COLUMN, database_steps_json)
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
    let revision: u32 = row.get(REVISION_COLUMN);
    let applied: u32 = row.get(APPLIED_COLUMN);
    let rolled_back: u32 = row.get(ROLLED_BACK_COLUMN);
    let errors_json: String = row.get(ERRORS_COLUMN);
    let errors: Vec<String> = serde_json::from_str(&errors_json).unwrap();
    let finished_at: Option<i64> = row.get(FINISHED_AT_COLUMN);
    let database_steps_json: String = row.get(DATABASE_STEPS_COLUMN);
    Migration {
        name: row.get(NAME_COLUMN),
        revision: revision as usize,
        datamodel: Schema::empty(),
        status: MigrationStatus::from_str(row.get(STATUS_COLUMN)),
        applied: applied as usize,
        rolled_back: rolled_back as usize,
        datamodel_steps: Vec::new(),
        database_steps: database_steps_json,
        errors: errors,
        started_at: timestamp_to_datetime(row.get(STARTED_AT_COLUMN)),
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
static DATABASE_STEPS_COLUMN: &str = "database_steps";
static ERRORS_COLUMN: &str = "errors";
static STARTED_AT_COLUMN: &str = "started_at";
static FINISHED_AT_COLUMN: &str = "finished_at";
