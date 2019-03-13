use crate::prelude::*;
use once_cell::sync::OnceCell;
use prisma_query::ast::{Column, Table};
use std::sync::{Arc, Weak};

pub type ModelRef = Arc<Model>;
pub type ModelWeakRef = Weak<Model>;

#[derive(Debug, Deserialize, Serialize)]
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
    pub manifestation: Option<ModelManifestation>,

    fields: OnceCell<Fields>,

    #[debug_stub = "#SchemaWeakRef#"]
    pub schema: SchemaWeakRef,
}

#[derive(Debug, Deserialize, Serialize)]
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

        let fields = Fields::new(
            self.fields
                .into_iter()
                .map(|fi| fi.build(Arc::downgrade(&model)))
                .collect(),
        );

        // The model is created here and fields WILL BE UNSET before now!
        model.fields.set(fields).unwrap();

        model
    }
}

impl Model {
    pub fn table(&self) -> Table {
        (self.schema().db_name.as_str(), self.db_name()).into()
    }

    pub fn fields(&self) -> &Fields {
        self.fields
            .get()
            .ok_or_else(|| String::from("Model fields must be set!"))
            .unwrap()
    }

    pub fn is_legacy(&self) -> bool {
        self.schema().is_legacy()
    }

    pub fn db_name(&self) -> &str {
        self.db_name_opt().unwrap_or_else(|| self.name.as_ref())
    }

    pub fn db_name_opt(&self) -> Option<&str> {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
    }

    pub fn schema(&self) -> SchemaRef {
        self.schema.upgrade().expect(
            "Schema does not exist anymore. Parent schema is deleted without deleting the child schema."
        )
    }

    pub fn id_column(&self) -> Column {
        self.fields().id().as_column()
    }
}
