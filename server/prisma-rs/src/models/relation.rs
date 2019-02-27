use crate::models::prelude::*;
use once_cell::unsync::OnceCell;
use std::sync::{Arc, Weak};

#[derive(Debug, Deserialize, Clone, Copy, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OnDelete {
    SetNull,
    Cascade,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationTemplate {
    pub name: String,
    pub model_a_on_delete: OnDelete,
    pub model_b_on_delete: OnDelete,

    #[serde(rename = "modelAId")]
    pub model_a_name: String,
    #[serde(rename = "modelBId")]
    pub model_b_name: String,
}

#[derive(DebugStub)]
pub struct Relation {
    pub name: String,

    model_a: OnceCell<RelationAttributes>,
    model_b: OnceCell<RelationAttributes>,

    #[debug_stub = "#SchemaWeakRef#"]
    pub schema: SchemaWeakRef,
}

#[derive(DebugStub)]
struct RelationAttributes {
    #[debug_stub = "#ModelWeakRef#"]
    pub model: ModelWeakRef,
    #[debug_stub = "#FieldWeakRef#"]
    pub field: Weak<RelationField>,
    pub on_delete: OnDelete,
}

impl RelationAttributes {
    pub fn new(model: ModelRef, field: Arc<RelationField>, on_delete: OnDelete) -> Self {
        RelationAttributes {
            model: Arc::downgrade(&model),
            field: Arc::downgrade(&field),
            on_delete: on_delete,
        }
    }
}

impl RelationTemplate {
    pub fn build(self, schema: SchemaWeakRef) -> Relation {
        let model_a_name = self.model_a_name;
        let model_b_name = self.model_b_name;

        let relation = Relation {
            name: self.name,
            model_a: OnceCell::new(),
            model_b: OnceCell::new(),
            schema: schema,
        };

        let model_a = {
            let model = relation.with_schema(|schema| schema.find_model(&model_a_name).unwrap());

            let field = model
                .fields()
                .find_from_relation(&relation.name, RelationSide::A)
                .unwrap();

            RelationAttributes::new(model, field, self.model_a_on_delete)
        };

        let model_b = {
            let model = relation.with_schema(|schema| schema.find_model(&model_b_name).unwrap());

            let field = model
                .fields()
                .find_from_relation(&relation.name, RelationSide::B)
                .unwrap();

            RelationAttributes::new(model, field, self.model_b_on_delete)
        };

        relation.model_a.set(model_a).unwrap();
        relation.model_b.set(model_b).unwrap();

        relation
    }
}

impl Relation {
    pub fn with_schema<F, T>(&self, f: F) -> T
    where
        F: FnOnce(SchemaRef) -> T,
    {
        match self.schema.upgrade(){
            Some(schema) => f(schema),
            None => panic!(
                "Schema does not exist anymore. Parent schema is deleted without deleting the child schema."
            )
        }
    }

    pub fn both_sides_cascade(&self) -> bool {
        let a = self.model_a.get().unwrap();
        let b = self.model_b.get().unwrap();

        a.on_delete == OnDelete::Cascade && b.on_delete == OnDelete::Cascade
    }

    pub fn model_a(&self) -> ModelRef {
        self.model_a
            .get()
            .unwrap()
            .model
            .upgrade()
            .expect("Model A deleted without deleting the relations in schama.")
    }

    pub fn model_b(&self) -> ModelRef {
        self.model_b
            .get()
            .unwrap()
            .model
            .upgrade()
            .expect("Model A deleted without deleting the relations in schama.")
    }
}
