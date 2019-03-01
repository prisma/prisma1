use super::FieldManifestation;
use crate::prelude::*;
use prisma_query::ast::*;
use std::sync::Arc;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationFieldTemplate {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub manifestation: Option<FieldManifestation>,
    pub relation_name: String,
    pub relation_side: RelationSide,
}

#[derive(DebugStub)]
pub struct RelationField {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub relation_name: String,
    pub relation_side: RelationSide,
    #[debug_stub = "#ModelWeakRef#"]
    pub model: ModelWeakRef,
    pub relation: RelationWeakRef,
}

#[derive(Clone, Copy, Debug, Deserialize, PartialEq)]
pub enum RelationSide {
    A,
    B,
}

impl RelationSide {
    pub fn opposite(self) -> RelationSide {
        match self {
            RelationSide::A => RelationSide::B,
            RelationSide::B => RelationSide::A,
        }
    }
}

impl RelationField {

    fn model(&self) -> ModelRef {
        self.model.upgrade().unwrap_or(
            panic!("Model does not exist anymore. Parent model got deleted without deleting the child.")
        )
    }

    fn relation(&self) -> RelationRef {
        self.relation.upgrade().unwrap_or(
            panic!("Relation does not exist anymore. Parent relation is deleted without deleting the child fields.")
        )
    }

    pub fn db_name(&self) -> String {
        let relation = self.relation();
        match relation.manifestation {
            Some(RelationLinkManifestation::Inline(ref m)) => {
                let is_self_rel = relation.is_self_relation();

                if is_self_rel
                    && (self.relation_side == RelationSide::B || self.related_field().is_hidden)
                {
                    m.referencing_column.clone()
                } else if is_self_rel && self.relation_side == RelationSide::A {
                    self.name.clone()
                } else if m.in_table_of_model_name == self.model().name {
                    m.referencing_column.clone()
                } else {
                    self.name.clone()
                }
            }
            _ => self.name.clone(),
        }
    }

    pub fn model_id_column(&self) -> Column {
        let model = self.model();
        let schema = model.schema();
        let db_name = schema.db_name.as_str();
        let table_name = model.db_name();
        let id_field = model.fields().id();
        let id_name = id_field.db_name();

        (db_name, table_name, id_name).into()
    }

    pub fn related_model(&self) -> ModelRef {
        match self.relation_side {
            RelationSide::A => self.relation().model_b(),
            RelationSide::B => self.relation().model_a(),
        }
    }

    pub fn related_field(&self) -> Arc<RelationField> {
        match self.relation_side {
            RelationSide::A => self.relation().field_b(),
            RelationSide::B => self.relation().field_a(),
        }
    }

    pub fn is_relation_with_name_and_side(&self, relation_name: &str, side: RelationSide) -> bool {
        self.relation().name == relation_name && self.relation_side == side
    }
}
