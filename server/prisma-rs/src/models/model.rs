use crate::models::{Field, FieldTemplate, Renameable, ScalarField, Schema, SchemaWeakRef};

use std::{
    collections::HashSet,
    rc::{Rc, Weak},
};

use once_cell::unsync::OnceCell;

pub type ModelRef = Rc<Model>;
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
    pub fields: OnceCell<Vec<Field>>,
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
        let model = Rc::new(Model {
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
            .map(|fi| fi.build(Rc::downgrade(&model)))
            .collect();

        // The model is created here and fields WILL BE UNSET before now!
        model.fields.set(fields).unwrap();

        model
    }
}

impl Renameable for Model {
    fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}

impl Model {
    fn with_schema<F, T>(&self, f: F) -> T
    where
        F: FnOnce(Rc<Schema>) -> T,
    {
        match self.schema.upgrade() {
            Some(model) => f(model),
            None => panic!(
                "Schema does not exist anymore. Parent schema is deleted without deleting the child models."
            )
        }
    }

    pub fn find_field(&self, name: &str) -> Option<&ScalarField> {
        self.scalar_fields()
            .iter()
            .find(|field| field.db_name() == name)
            .cloned()
    }

    pub fn find_fields(&self, names: &HashSet<&str>) -> Vec<&ScalarField> {
        self.scalar_fields()
            .into_iter()
            .filter(|field| names.contains(field.db_name()))
            .collect()
    }

    pub fn scalar_fields(&self) -> Vec<&ScalarField> {
        match self.fields.get() {
            Some(fields) => fields.iter().fold(Vec::new(), |mut acc, field| {
                if let Field::Scalar(scalar_field) = field {
                    acc.push(scalar_field);
                }

                acc
            }),
            None => Vec::with_capacity(0),
        }
    }

    pub fn is_legacy(&self) -> bool {
        self.with_schema(|schema| schema.is_legacy())
    }
}
