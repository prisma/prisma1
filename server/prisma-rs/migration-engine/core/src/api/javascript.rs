mod migration_task;

use super::{GenericApi, MigrationApi};
use crate::commands::*;
use datamodel::{self, Configuration, SerializeableMcf};
use migration_task::*;
use neon::prelude::*;
use sql_migration_connector::SqlMigrationConnector;
use std::sync::Arc;

pub struct JavascriptApi {
    inner: Arc<dyn GenericApi>,
}

impl JavascriptApi {
    pub fn new(config: &str) -> crate::Result<Self> {
        let config = datamodel::load_configuration(config)?;

        let source = config.datasources.first().ok_or(CommandError::DataModelErrors {
            code: 1000,
            errors: vec!["There is no datasource in the configuration.".to_string()],
        })?;

        let connector = match source.connector_type().as_ref() {
            "sqlite" => SqlMigrationConnector::sqlite(&source.url())?,
            "postgresql" => SqlMigrationConnector::postgres(&source.url())?,
            "mysql" => SqlMigrationConnector::mysql(&source.url())?,
            x => unimplemented!("Connector {} is not supported yet", x),
        };

        Ok(Self {
            inner: Arc::new(MigrationApi::new(connector)?),
        })
    }

    pub fn create_task<'a, T>(&self, input: T::Input) -> T
    where
        T: MigrationTask<'a>,
    {
        T::create(Arc::clone(&self.inner), input)
    }
}

declare_types! {
    pub class JavascriptApiExport for JavascriptApi {
        init(mut cx) {
            let config: String = cx.argument::<JsString>(0)?.value();
            Ok(JavascriptApi::new(&config).unwrap())
        }

        method get_config(mut cx) {
            let model: String = cx.argument::<JsString>(0)?.value();

            let config = datamodel::load_configuration(&model).unwrap();
            let js_value = neon_serde::to_value(&mut cx, &config.to_serializeable())?;

            Ok(js_value)
        }

        method dmmf_to_dml(mut cx) {
            let dmmf: String = cx.argument::<JsString>(0)?.value();
            let arg1 = cx.argument::<JsValue>(1)?;

            let dml = {
                let mcf: SerializeableMcf = neon_serde::from_value(&mut cx, arg1)?;
                let config = Configuration::from(mcf);

                let model = datamodel::dmmf::parse_from_dmmf(&dmmf);
                datamodel::render_with_config(&model, &config).unwrap()
            };

            Ok(cx.string(dml).upcast())
        }

        method apply_migration(mut cx) {
            {
                let this = cx.this();
                let input = cx.argument::<JsValue>(0)?;
                let cb = cx.argument::<JsFunction>(1)?;

                let input: ApplyMigrationInput = neon_serde::from_value(&mut cx, input)?;
                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<ApplyMigrationTask>(input);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }

        method calculate_database_steps(mut cx) {
            {
                let this = cx.this();
                let input = cx.argument::<JsValue>(0)?;
                let cb = cx.argument::<JsFunction>(1)?;

                let input: CalculateDatabaseStepsInput = neon_serde::from_value(&mut cx, input)?;
                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<CalculateDatabaseStepsTask>(input);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }

        method calculate_datamodel(mut cx) {
            {
                let this = cx.this();
                let input = cx.argument::<JsValue>(0)?;
                let cb = cx.argument::<JsFunction>(1)?;

                let input: CalculateDatamodelInput = neon_serde::from_value(&mut cx, input)?;
                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<CalculateDatamodelTask>(input);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }

        method infer_migration_steps(mut cx) {
            {
                let this = cx.this();
                let input = cx.argument::<JsValue>(0)?;
                let cb = cx.argument::<JsFunction>(1)?;

                let input: InferMigrationStepsInput = neon_serde::from_value(&mut cx, input)?;
                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<InferMigrationStepsTask>(input);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }

        method list_migrations(mut cx) {
            {
                let this = cx.this();
                let input = cx.argument::<JsValue>(0)?;
                let cb = cx.argument::<JsFunction>(1)?;

                let input: ListMigrationStepsInput = neon_serde::from_value(&mut cx, input)?;

                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<ListMigrationStepsTask>(input);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }

        method migration_progress(mut cx) {
            {
                let this = cx.this();
                let input = cx.argument::<JsValue>(0)?;
                let cb = cx.argument::<JsFunction>(1)?;

                let input: MigrationProgressInput = neon_serde::from_value(&mut cx, input)?;

                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<MigrationProgressTask>(input);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }

        method reset(mut cx) {
            {
                let this = cx.this();
                let cb = cx.argument::<JsFunction>(0)?;

                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<ResetTask>(serde_json::Value::Null);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }

        method unapply_migration(mut cx) {
            {
                let this = cx.this();
                let input = cx.argument::<JsValue>(0)?;
                let cb = cx.argument::<JsFunction>(1)?;

                let input: UnapplyMigrationInput = neon_serde::from_value(&mut cx, input)?;

                let guard = cx.lock();
                let api = this.borrow(&guard);

                let task = api.create_task::<UnapplyMigrationTask>(input);
                task.schedule(cb);
            }

            Ok(cx.undefined().upcast())
        }
    }
}
