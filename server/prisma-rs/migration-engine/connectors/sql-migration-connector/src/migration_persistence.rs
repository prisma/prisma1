#[allow(unused, dead_code)]
use chrono::*;
use datamodel::Schema;
use migration_connector::*;
use prisma_query::{ast::*, error::Error as SqlError, transaction::Connection, visitor::*, convenience::*};
use serde_json;

pub struct SqlMigrationPersistence<'a> {
    // We need to tie the connection lifetime to the struct,
    // and rust forces us to use the lifetime specifier in the
    // struct declaration.
    phantom: std::marker::PhantomData<&'a Connection>,
}

impl<'a> SqlMigrationPersistence<'a> {
    pub fn new() -> SqlMigrationPersistence<'a> {
        SqlMigrationPersistence { phantom: std::marker::PhantomData }
    }
}

#[allow(unused, dead_code)]
impl<'a> MigrationPersistence for SqlMigrationPersistence<'a> {
    type ErrorType = SqlError;
    type ConnectionType = &'a mut Connection;

    fn last(&self, connection: &'a mut Connection) -> Result<Migration, SqlError> {
        let conditions = STATUS_COLUMN.equals("Success");
        let query = Select::from_table(TABLE_NAME)
            .so_that(conditions)
            .order_by(REVISION_COLUMN.descend());

        let (cols, vals) = connection.query(Query::from(query))?;
        let res = ResultSet::new(&cols, &vals);

        for row in res.iter() {
            return parse_row(&row);
        }

        Err(SqlError::NotFound)
    }

    fn load_all(&self, connection: &mut Connection) -> Result<Vec<Migration>, SqlError> {
        let query = Select::from_table(TABLE_NAME);

        let (cols, vals) = connection.query(Query::from(query))?;
        let res = ResultSet::new(&cols, &vals);

        let mut result: Vec<Migration> = vec![];

        for row in res.iter() {
            result.push(parse_row(&row)?);
        }

        Ok(result)
    }

    fn by_name(&self, connection: &mut Connection, name: &str) -> Result<Migration, SqlError> {
        let conditions = NAME_COLUMN.equals(name);
        let query = Select::from_table(TABLE_NAME)
            .so_that(conditions)
            .order_by(REVISION_COLUMN.descend());

        let (cols, vals) = connection.query(Query::from(query))?;
        let res = ResultSet::new(&cols, &vals);

        for row in res.iter() {
            return parse_row(&row);
        }

        Err(SqlError::NotFound)
    }

    fn create(&self, connection: &mut Connection, migration: Migration) -> Result<Migration, SqlError> {
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

        cloned.revision = match connection.execute(Query::from(query))? {
            Some(Id::Int(val)) => val,
            _ => panic!("Impossible ID")
        };
        Ok(cloned)
    }

    fn update(&self, connection: &mut Connection, params: &MigrationUpdateParams) -> Result<Migration, SqlError> {
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
                Column::from(NAME_COLUMN)
                    .equals(&params.name as &str)
                    .and(REVISION_COLUMN.equals(params.revision)),
            );

        connection.execute(Query::from(query))?;

        self.by_name(connection, &params.name)
    }
}

fn timestamp_to_datetime(timestamp: i64) -> DateTime<Utc> {
    let nsecs = ((timestamp % 1000) * 1_000_000) as u32;
    let secs = (timestamp / 1000) as i64;
    let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
    let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

    datetime
}

fn parse_row(row: &ResultRowWithName) -> Result<Migration, SqlError> {
    let revision: u32 = row.get_as_integer(REVISION_COLUMN)? as u32;
    let applied: u32 = row.get_as_integer(APPLIED_COLUMN)?as u32;
    let rolled_back: u32 = row.get_as_integer(ROLLED_BACK_COLUMN)? as u32;
    let errors_json: String = row.get_as_string(ERRORS_COLUMN)?;
    let errors: Vec<String> = serde_json::from_str(&errors_json).unwrap();
    let finished_at: Option<i64> = row.get_as_integer(FINISHED_AT_COLUMN).ok();
    let database_steps_json: String = row.get_as_string(DATABASE_STEPS_COLUMN)?;
    Ok(Migration {
        name: row.get_as_string(NAME_COLUMN)?,
        revision: revision as usize,
        datamodel: Schema::empty(),
        status: MigrationStatus::from_str(row.get_as_string(STATUS_COLUMN)?),
        applied: applied as usize,
        rolled_back: rolled_back as usize,
        datamodel_steps: Vec::new(),
        database_steps: database_steps_json,
        errors: errors,
        started_at: timestamp_to_datetime(row.get_as_integer(STARTED_AT_COLUMN)?),
        finished_at: finished_at.map(timestamp_to_datetime),
    })
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
