use crate::{
    models::{
        ScalarField,
        Field,
        Renameable,
        SchemaRef,
        FieldTemplate
    }
};

use std::{
    cell::RefCell,
    rc::Rc,
};

pub type ModelRef = Rc<RefCell<Model>>;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelTemplate {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub fields: Vec<FieldTemplate>,
    pub manifestation: Option<ModelManifestation>,
}

#[derive(Debug)]
pub struct Model {
    pub name: String,
    pub stable_identifier: String,
    pub is_embedded: bool,
    pub fields: Vec<Field>,
    pub manifestation: Option<ModelManifestation>,
    pub schema: SchemaRef,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelManifestation {
    pub db_name: String,
}

impl ModelTemplate {
    pub fn build(self, schema: SchemaRef) -> ModelRef {
        let model = Rc::new(RefCell::new(Model {
            name: self.name,
            stable_identifier: self.stable_identifier,
            is_embedded: self.is_embedded,
            fields: Vec::new(),
            manifestation: self.manifestation,
            schema,
        }));

        self.fields
            .into_iter()
            .for_each(|fi| model.borrow_mut().fields.push(fi.build(model.clone())));

        model
    }
}

impl Model {
    pub fn add_field(&mut self, field: Field) {
        self.fields.push(field);
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
    pub fn find_field(&self, name: &str) -> Option<&ScalarField> {
        self.scalar_fields()
            .iter()
            .find(|field| field.db_name() == name)
            .cloned()
    }

    pub fn scalar_fields(&self) -> Vec<&ScalarField> {
        self.fields.iter().fold(Vec::new(), |mut acc, field| {
            if let Field::Scalar(scalar_field) = field {
                acc.push(scalar_field);
            }

            acc
        })
    }

    pub fn is_legacy(&self) -> bool {
        self.schema.borrow().is_legacy()
    }
}
