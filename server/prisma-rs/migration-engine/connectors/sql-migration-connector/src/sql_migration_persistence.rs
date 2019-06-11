#[allow(unused, dead_code)]
use barrel::types;
use chrono::*;
use migration_connector::*;
use prisma_query::{Connectional, ResultSet};
use prisma_query::ast::*;
use serde_json;
use std::sync::Arc;

pub struct SqlMigrationPersistence<C: Connectional> {
    pub connection: Arc<C>,
    pub schema_name: String,
    pub folder_path: Option<String>,
}

#[allow(unused, dead_code)]
impl<C: Connectional> MigrationPersistence for SqlMigrationPersistence<C> {
    fn init(&self) {
        println!("SqlMigrationPersistence.init()");
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

        let sql_str = dbg!(m.make::<barrel::backend::Sqlite>());

        self
            .connection
            .with_connection(&self.schema_name, |conn| conn.query_raw(&sql_str, &[]))
            .unwrap();
    }

    fn reset(&self) {
        println!("SqlMigrationPersistence.reset()");
        let sql_str = format!(r#"DELETE FROM "{}"."_Migration";"#, self.schema_name); // TODO: this is not vendor agnostic yet
        let _ = self.connection.with_connection(&self.schema_name, |conn| conn.query_raw(&sql_str, &[]));

        if let Some(ref folder_path) = self.folder_path {
            let mut file_path = format!("{}/{}.db", folder_path, self.schema_name);
            let _ = dbg!(std::fs::remove_file(file_path)); // ignore potential errors
        }
    }

    fn last(&self) -> Option<Migration> {
        let conditions = STATUS_COLUMN.equals("Success");
        let query = Select::from_table(self.table())
            .so_that(conditions)
            .order_by(REVISION_COLUMN.descend());

        self.connection
            .with_connection(&self.schema_name, |conn| {
                let result_set = conn.query(query.into()).unwrap();
                Ok(parse_rows_new(&result_set).into_iter().next())
            })
            .unwrap()
    }

    fn load_all(&self) -> Vec<Migration> {
        let query = Select::from_table(self.table());

        self.connection
            .with_connection(&self.schema_name, |conn| {
                let result_set = conn.query(query.into()).unwrap();
                Ok(parse_rows_new(&result_set))
            })
            .unwrap()
    }

    fn by_name(&self, name: &str) -> Option<Migration> {
        let conditions = NAME_COLUMN.equals(name);
        let query = Select::from_table(self.table())
            .so_that(conditions)
            .order_by(REVISION_COLUMN.descend());

        self.connection
            .with_connection(&self.schema_name, |conn| {
                let result_set = conn.query(query.into()).unwrap();
                Ok(parse_rows_new(&result_set).into_iter().next())
            })
            .unwrap()
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

        let query = Insert::single_into(self.table())
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

        self.connection
            .with_connection(&self.schema_name, |conn| {
                let id = conn.execute(query.into())?;
                match id {
                    Some(prisma_query::ast::Id::Int(id)) => cloned.revision = id,
                    _ => panic!("This insert must return an int"),
                }
                Ok(cloned)
            })
            .unwrap()
    }

    fn update(&self, params: &MigrationUpdateParams) {
        let finished_at_value = match params.finished_at {
            Some(x) => x.timestamp_millis().into(),
            None => ParameterizedValue::Null,
        };
        let errors_json = serde_json::to_string(&params.errors).unwrap();
        let query = Update::table(self.table())
            .set(NAME_COLUMN, params.new_name.clone())
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

        self.connection
            .with_connection(&self.schema_name, |conn| {
                conn.query(query.into())?;
                Ok(())
            })
            .unwrap()
    }    
}

impl<C: Connectional> SqlMigrationPersistence<C> {
    fn table(&self) -> Table {
        if self.folder_path.is_some() {
            // sqlite case
            TABLE_NAME.to_string().into()
        } else {
            (self.schema_name.to_string(), TABLE_NAME.to_string()).into()
        }
    }
}

fn timestamp_to_datetime(timestamp: i64) -> DateTime<Utc> {
    let nsecs = ((timestamp % 1000) * 1_000_000) as u32;
    let secs = (timestamp / 1000) as i64;
    let naive = chrono::NaiveDateTime::from_timestamp(secs, nsecs);
    let datetime: DateTime<Utc> = DateTime::from_utc(naive, Utc);

    datetime
}

fn parse_rows_new(result_set: &ResultSet) -> Vec<Migration> {

    result_set
        .into_iter()
        .map(|row| {
            let datamodel_string: String = row.get_as_string(DATAMODEL_COLUMN).unwrap();
            let datamodel_steps_json: String = row.get_as_string(DATAMODEL_STEPS_COLUMN).unwrap();
            let database_migration_string: String = row.get_as_string(DATABASE_MIGRATION_COLUMN).unwrap();
            let errors_json: String = row.get_as_string(ERRORS_COLUMN).unwrap();
            let finished_at: Option<i64> = match row.get(FINISHED_AT_COLUMN) {
                Ok(ParameterizedValue::Integer(v)) => Some(*v),
                Ok(ParameterizedValue::Null) => None,
                Ok(p) => panic!(format!("expectd an int value but got {:?}", p)),
                Err(err) => panic!(format!("{}", err)),
            };

            let datamodel_steps = serde_json::from_str(&datamodel_steps_json).unwrap();
            let datamodel = datamodel::parse(&datamodel_string).unwrap();
            let database_migration_json = serde_json::from_str(&database_migration_string).unwrap();
            let errors: Vec<String> = serde_json::from_str(&errors_json).unwrap();
            Migration {
                name: row.get_as_string(NAME_COLUMN).unwrap(),
                revision: row.get_as_integer(REVISION_COLUMN).unwrap() as usize,
                datamodel: datamodel,
                status: MigrationStatus::from_str(row.get_as_string(STATUS_COLUMN).unwrap()),
                applied: row.get_as_integer(APPLIED_COLUMN).unwrap() as usize,
                rolled_back: row.get_as_integer(ROLLED_BACK_COLUMN).unwrap() as usize,
                datamodel_steps: datamodel_steps,
                database_migration: database_migration_json,
                errors: errors,
                started_at: timestamp_to_datetime(row.get_as_integer(STARTED_AT_COLUMN).unwrap()),
                finished_at: finished_at.map(timestamp_to_datetime),
            }
        })
        .collect()
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
