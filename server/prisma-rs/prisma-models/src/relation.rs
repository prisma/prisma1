use crate::prelude::*;
use once_cell::unsync::OnceCell;
use prisma_common::{error::Error, PrismaResult};
use std::sync::{Arc, Weak};

pub type RelationRef = Arc<Relation>;
pub type RelationWeakRef = Weak<Relation>;

#[derive(Debug, Deserialize, Clone, Copy, PartialEq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OnDelete {
    SetNull,
    Cascade,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InlineRelation {
    #[serde(rename = "inTableOfModelId")]
    pub in_table_of_model_name: String,
    pub referencing_column: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationTable {
    pub table: String,
    pub model_a_column: String,
    pub model_b_column: String,
    pub id_column: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", tag = "relation_manifestation_type")]
pub enum RelationLinkManifestation {
    Inline(InlineRelation),
    RelationTable(RelationTable),
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationTemplate {
    pub name: String,
    pub model_a_on_delete: OnDelete,
    pub model_b_on_delete: OnDelete,
    pub manifestation: Option<RelationLinkManifestation>,

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

    pub manifestation: Option<RelationLinkManifestation>,

    #[debug_stub = "#SchemaWeakRef#"]
    pub schema: SchemaWeakRef,
}

#[derive(DebugStub)]
struct RelationAttributes {
    pub name: String,
    #[debug_stub = "#ModelWeakRef#"]
    pub model: ModelWeakRef,
    #[debug_stub = "#FieldWeakRef#"]
    pub field: Weak<RelationField>,
    pub on_delete: OnDelete,
}

impl RelationAttributes {
    pub fn new(
        name: String,
        model: ModelRef,
        field: Arc<RelationField>,
        on_delete: OnDelete,
    ) -> Self {
        RelationAttributes {
            name: name,
            model: Arc::downgrade(&model),
            field: Arc::downgrade(&field),
            on_delete: on_delete,
        }
    }
}

impl RelationTemplate {
    pub fn build(self, schema: SchemaWeakRef) -> RelationRef {
        let model_a_name = self.model_a_name;
        let model_b_name = self.model_b_name;

        let relation = Relation {
            name: self.name,
            manifestation: self.manifestation,
            model_a: OnceCell::new(),
            model_b: OnceCell::new(),
            schema: schema,
        };

        let model_a = {
            let model = relation.with_schema(|schema| schema.find_model(&model_a_name).unwrap());
            let field = model.fields().find_from_relation(&relation.name).unwrap();

            RelationAttributes::new(model_a_name, model, field, self.model_a_on_delete)
        };

        let model_b = {
            let model = relation.with_schema(|schema| schema.find_model(&model_b_name).unwrap());
            let field = model.fields().find_from_relation(&relation.name).unwrap();

            RelationAttributes::new(model_b_name, model, field, self.model_b_on_delete)
        };

        relation.model_a.set(model_a).unwrap();
        relation.model_b.set(model_b).unwrap();

        Arc::new(relation)
    }
}

impl Relation {
    const MODEL_A_DEFAULT_COLUMN: &'static str = "A";
    const MODEL_B_DEFAULT_COLUMN: &'static str = "B";

    fn with_schema<F, T>(&self, f: F) -> T
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

    pub fn is_inline_relation(&self) -> bool {
        self.manifestation
            .as_ref()
            .map(|manifestation| match manifestation {
                RelationLinkManifestation::RelationTable(_) => true,
                _ => false,
            })
            .unwrap_or(false)
    }

    pub fn is_relation_table(&self) -> bool {
        !self.is_inline_relation()
    }

    pub fn is_self_relation(&self) -> bool {
        self.model_a().name == self.model_b().name
    }

    pub fn inline_manifestation(&self) -> Option<&InlineRelation> {
        use RelationLinkManifestation::*;

        match self.manifestation {
            Some(Inline(ref m)) => Some(m),
            _ => None,
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

    pub fn field_a(&self) -> Arc<RelationField> {
        self.model_a
            .get()
            .unwrap()
            .field
            .upgrade()
            .expect("Field A deleted without deleting the relations in schama.")
    }

    pub fn model_b(&self) -> ModelRef {
        self.model_b
            .get()
            .unwrap()
            .model
            .upgrade()
            .expect("Model B deleted without deleting the relations in schama.")
    }

    pub fn field_b(&self) -> Arc<RelationField> {
        self.model_b
            .get()
            .unwrap()
            .field
            .upgrade()
            .expect("Field A deleted without deleting the relations in schama.")
    }

    pub fn relation_table_name(&self) -> String {
        use RelationLinkManifestation::*;

        match self.manifestation {
            Some(RelationTable(ref m)) => m.table.clone(),
            Some(Inline(ref m)) => self.with_schema(|schema| {
                schema
                    .find_model(&m.in_table_of_model_name)
                    .unwrap()
                    .db_name()
                    .to_string()
            }),
            None => format!("_{}", self.name),
        }
    }

    pub fn model_a_column(&self) -> String {
        use RelationLinkManifestation::*;

        match self.manifestation {
            Some(RelationTable(ref m)) => m.model_a_column.clone(),
            Some(Inline(ref m)) => {
                let model = self.model_a();

                if m.in_table_of_model_name == model.name && !self.is_self_relation() {
                    model.fields().id().db_name().to_string()
                } else {
                    m.referencing_column.to_string()
                }
            }
            None => Self::MODEL_A_DEFAULT_COLUMN.to_string(),
        }
    }

    pub fn model_b_column(&self) -> String {
        use RelationLinkManifestation::*;

        match self.manifestation {
            Some(RelationTable(ref m)) => m.model_b_column.clone(),
            Some(Inline(ref m)) => {
                let model = self.model_b();

                if m.in_table_of_model_name == model.name && !self.is_self_relation() {
                    model.fields().id().db_name().to_string()
                } else {
                    m.referencing_column.to_string()
                }
            }
            None => Self::MODEL_B_DEFAULT_COLUMN.to_string(),
        }
    }

    pub fn is_many_to_many(&self) -> bool {
        self.field_a().is_list && self.field_b().is_list
    }

    pub fn id_column(&self) -> Option<&str> {
        use RelationLinkManifestation::*;

        match self.manifestation {
            None => Some("id"),
            Some(RelationTable(ref m)) => Some(m.id_column.as_ref()),
            _ => None,
        }
    }

    pub fn relation_table_has_3_columns(&self) -> bool {
        self.id_column().is_some()
    }

    pub fn column_for_relation_side(&self, side: RelationSide) -> String {
        match side {
            RelationSide::A => self.model_a_column(),
            RelationSide::B => self.model_b_column(),
        }
    }

    pub fn contains_the_model(&self, model: ModelRef) -> bool {
        self.model_a().name == model.name || self.model_b().name == model.name
    }

    pub fn get_field_on_model(&self, model_id: &str) -> PrismaResult<Arc<RelationField>> {
        if model_id == self.model_a().name {
            Ok(self.field_a())
        } else if model_id == self.model_b().name {
            Ok(self.field_b())
        } else {
            Err(Error::InvalidInputError(format!(
                "The model id {} is not part of the relation {}",
                model_id, self.name
            )))
        }
    }
}
