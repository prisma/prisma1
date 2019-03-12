use crate::prelude::*;
use once_cell::unsync::OnceCell;
use prisma_common::{error::Error, PrismaResult};
use prisma_query::ast::{Column, Table};
use std::sync::{Arc, Weak};

pub type RelationRef = Arc<Relation>;
pub type RelationWeakRef = Weak<Relation>;

#[derive(Debug, Deserialize, Serialize, Clone, Copy, PartialEq, Eq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum OnDelete {
    SetNull,
    Cascade,
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InlineRelation {
    #[serde(rename = "inTableOfModelId")]
    pub in_table_of_model_name: String,
    pub referencing_column: String,
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationTable {
    pub table: String,
    pub model_a_column: String,
    pub model_b_column: String,
    pub id_column: Option<String>,
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "snake_case", tag = "relationManifestationType")]
pub enum RelationLinkManifestation {
    Inline(InlineRelation),
    RelationTable(RelationTable),
}

#[derive(Debug, Deserialize, Serialize)]
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

    model_a_name: String,
    model_b_name: String,

    pub model_a_on_delete: OnDelete,
    pub model_b_on_delete: OnDelete,

    model_a: OnceCell<ModelWeakRef>,
    model_b: OnceCell<ModelWeakRef>,

    field_a: OnceCell<Weak<RelationField>>,
    field_b: OnceCell<Weak<RelationField>>,

    pub manifestation: Option<RelationLinkManifestation>,

    #[debug_stub = "#SchemaWeakRef#"]
    pub schema: SchemaWeakRef,
}

impl RelationTemplate {
    pub fn build(self, schema: SchemaWeakRef) -> RelationRef {
        let relation = Relation {
            name: self.name,
            manifestation: self.manifestation,
            model_a_name: self.model_a_name,
            model_b_name: self.model_b_name,
            model_a_on_delete: self.model_a_on_delete,
            model_b_on_delete: self.model_b_on_delete,
            model_a: OnceCell::new(),
            model_b: OnceCell::new(),
            field_a: OnceCell::new(),
            field_b: OnceCell::new(),
            schema: schema,
        };

        Arc::new(relation)
    }
}

impl Relation {
    pub const MODEL_A_DEFAULT_COLUMN: &'static str = "A";
    pub const MODEL_B_DEFAULT_COLUMN: &'static str = "B";
    pub const TABLE_ALIAS: &'static str = "RelationTable";

    fn schema(&self) -> SchemaRef {
        self.schema
            .upgrade()
            .expect("Schema does not exist anymore. Parent schema is deleted without deleting the child schema.")
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
        self.model_a_on_delete == OnDelete::Cascade && self.model_b_on_delete == OnDelete::Cascade
    }

    pub fn model_a(&self) -> ModelRef {
        self.model_a
            .get_or_init(|| {
                let model = self.schema().find_model(&self.model_a_name).unwrap();
                Arc::downgrade(&model)
            })
            .upgrade()
            .expect("Model A deleted without deleting the relations in schema.")
    }

    pub fn field_a(&self) -> Arc<RelationField> {
        self.field_a
            .get_or_init(|| {
                let field = self.model_a().fields().find_from_relation(&self.name).unwrap();

                Arc::downgrade(&field)
            })
            .upgrade()
            .expect("Field A deleted without deleting the relations in schema.")
    }

    pub fn model_b(&self) -> ModelRef {
        self.model_b
            .get_or_init(|| {
                let model = self.schema().find_model(&self.model_b_name).unwrap();
                Arc::downgrade(&model)
            })
            .upgrade()
            .expect("Model B deleted without deleting the relations in schema.")
    }

    pub fn field_b(&self) -> Arc<RelationField> {
        self.field_b
            .get_or_init(|| {
                let field = self.model_b().fields().find_from_relation(&self.name).unwrap();

                Arc::downgrade(&field)
            })
            .upgrade()
            .expect("Field B deleted without deleting the relations in schema.")
    }

    pub fn relation_table(&self) -> Table {
        use RelationLinkManifestation::*;

        match self.manifestation {
            Some(RelationTable(ref m)) => m.table.clone().into(),
            Some(Inline(ref m)) => self
                .schema()
                .find_model(&m.in_table_of_model_name)
                .unwrap()
                .db_name()
                .into(),
            None => format!("_{}", self.name).into(),
        }
    }

    pub fn model_a_column(&self) -> Column {
        use RelationLinkManifestation::*;

        match self.manifestation {
            Some(RelationTable(ref m)) => m.model_a_column.clone().into(),
            Some(Inline(ref m)) => {
                let model = self.model_a();

                if m.in_table_of_model_name == model.name && !self.is_self_relation() {
                    model.fields().id().as_column()
                } else {
                    let column: &str = m.referencing_column.as_ref();
                    column.into()
                }
            }
            None => Self::MODEL_A_DEFAULT_COLUMN.into(),
        }
    }

    pub fn model_b_column(&self) -> Column {
        use RelationLinkManifestation::*;

        match self.manifestation {
            Some(RelationTable(ref m)) => m.model_b_column.clone().into(),
            Some(Inline(ref m)) => {
                let model = self.model_b();
                let id = model.fields().id();

                if m.in_table_of_model_name == model.name && !self.is_self_relation() {
                    id.as_column()
                } else {
                    let column: &str = m.referencing_column.as_ref();
                    column.into()
                }
            }
            None => Self::MODEL_B_DEFAULT_COLUMN.into(),
        }
    }

    pub fn is_many_to_many(&self) -> bool {
        self.field_a().is_list && self.field_b().is_list
    }

    pub fn id_column(&self) -> Option<&str> {
        use RelationLinkManifestation::*;

        match self.manifestation {
            None => Some("id"),
            Some(RelationTable(ref m)) => m.id_column.as_ref().map(|s| s.as_ref()),
            _ => None,
        }
    }

    pub fn relation_table_has_3_columns(&self) -> bool {
        self.id_column().is_some()
    }

    pub fn column_for_relation_side(&self, side: RelationSide) -> Column {
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
