use crate::models::{FieldTemplate, Fields, Schema, SchemaWeakRef};

use once_cell::unsync::OnceCell;
use std::sync::{Arc, Weak};

pub type ModelRef = Arc<Model>;
pub type ModelWeakRef = Weak<Model>;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelTemplate {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub fields: Vec<FieldTemplate>,
    pub manifestation: Option<ModelManifestation>,
}

#[derive(DebugStub)]
pub struct Model {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    fields: OnceCell<Fields>,
    pub manifestation: Option<ModelManifestation>,
    #[debug_stub = "#SchemaWeakRef#"]
    pub schema: SchemaWeakRef,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelManifestation {
    pub db_name: String,
}

impl ModelTemplate {
    pub fn build(self, schema: SchemaWeakRef) -> ModelRef {
        let model = Arc::new(Model {
            name: self.name,
            stable_identifier: self.stable_identifier,
            is_embedded: self.is_embedded,
            fields: OnceCell::new(),
            manifestation: self.manifestation,
            schema,
        });

        let fields = self
            .fields
            .into_iter()
            .map(|fi| fi.build(Arc::downgrade(&model)))
            .collect();

        // The model is created here and fields WILL BE UNSET before now!
        model.fields.set(Fields::new(fields)).unwrap();

        model
    }
}

impl Model {
    fn with_schema<F, T>(&self, f: F) -> T
    where
        F: FnOnce(Arc<Schema>) -> T,
    {
        match self.schema.upgrade() {
            Some(model) => f(model),
            None => panic!(
                "Schema does not exist anymore. Parent schema is deleted without deleting the child models."
            )
        }
    }

    pub fn fields(&self) -> &Fields {
        self.fields
            .get()
            .ok_or_else(|| String::from("Model fields must be set!"))
            .unwrap()
    }

    pub fn is_legacy(&self) -> bool {
        self.with_schema(|schema| schema.is_legacy())
    }

    pub fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}
