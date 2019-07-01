use super::FieldManifestation;
use crate::prelude::*;
use once_cell::sync::OnceCell;
use prisma_query::ast::Column;
use std::sync::{Arc, Weak};

pub type RelationFieldRef = Arc<RelationField>;
pub type RelationFieldWeak = Weak<RelationField>;

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RelationFieldTemplate {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
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
    pub is_hidden: bool,
    pub is_auto_generated: bool,
    pub relation_name: String,
    pub relation_side: RelationSide,
    #[debug_stub = "#ModelWeakRef#"]
    pub model: ModelWeakRef,
    pub relation: OnceCell<RelationWeakRef>,

    is_unique: bool,
}

#[derive(Clone, Copy, Debug, Deserialize, Serialize, PartialEq)]
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

    pub fn is_a(&self) -> bool {
        *self == RelationSide::A
    }

    pub fn is_b(&self) -> bool {
        *self == RelationSide::B
    }
}

impl RelationField {
    pub fn new(
        name: String,
        type_identifier: TypeIdentifier,
        is_required: bool,
        is_list: bool,
        is_hidden: bool,
        is_auto_generated: bool,
        is_unique: bool,
        relation_name: String,
        relation_side: RelationSide,
        model: ModelWeakRef,
        relation: OnceCell<RelationWeakRef>,
    ) -> Self {
        RelationField {
            name,
            type_identifier,
            is_required,
            is_list,
            is_hidden,
            is_auto_generated,
            is_unique,
            relation_name,
            relation_side,
            model,
            relation,
        }
    }

    pub fn is_optional(&self) -> bool {
        !self.is_required
    }

    pub fn is_unique(&self) -> bool {
        self.is_unique
    }

    pub fn model(&self) -> ModelRef {
        self.model
            .upgrade()
            .expect("Model does not exist anymore. Parent model got deleted without deleting the child.")
    }

    pub fn relation(&self) -> RelationRef {
        self.relation
            .get_or_init(|| {
                self.model()
                    .internal_data_model()
                    .find_relation(&self.relation_name)
                    .unwrap()
            })
            .upgrade()
            .unwrap()
    }

    pub fn db_name(&self) -> String {
        let relation = self.relation();

        match relation.manifestation {
            Some(RelationLinkManifestation::Inline(ref m)) => {
                let is_self_rel = relation.is_self_relation();

                if is_self_rel && self.is_hidden {
                    self.name.clone()
                } else if is_self_rel && (self.relation_side == RelationSide::B || self.related_field().is_hidden) {
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

    pub fn relation_is_inlined_in_parent(&self) -> bool {
        let relation = self.relation();

        match relation.manifestation {
            Some(RelationLinkManifestation::Inline(ref m)) => {
                let is_self_rel = relation.is_self_relation();

                if is_self_rel && self.is_hidden {
                    false
                } else if is_self_rel && (self.relation_side == RelationSide::B || self.related_field().is_hidden) {
                    true
                } else if is_self_rel && self.relation_side == RelationSide::A {
                    false
                } else if m.in_table_of_model_name == self.model().name {
                    true
                } else {
                    false
                }
            }
            _ => false,
        }
    }

    pub fn opposite_column(&self) -> Column<'static> {
        match self.relation_side {
            RelationSide::A => self.relation().model_b_column(),
            RelationSide::B => self.relation().model_a_column(),
        }
    }

    pub fn relation_column(&self) -> Column<'static> {
        match self.relation_side {
            RelationSide::A => self.relation().model_a_column(),
            RelationSide::B => self.relation().model_b_column(),
        }
    }

    pub fn as_column(&self) -> Column<'static> {
        let model = self.model();
        let internal_data_model = model.internal_data_model();
        let db_name = self.db_name();
        let parts = (
            (internal_data_model.db_name.clone(), model.db_name().to_string()),
            db_name.clone(),
        );

        parts.into()
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
