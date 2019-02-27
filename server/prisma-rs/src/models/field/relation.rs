use prisma_query::ast::*;

use super::FieldManifestation;
use crate::models::prelude::*;
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
    pub manifestation: Option<FieldManifestation>,
    pub relation_name: String,
    pub relation_side: RelationSide,
    #[debug_stub = "#ModelWeakRef#"]
    pub model: ModelWeakRef,
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
    pub fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }

    pub fn with_model<F, T>(&self, f: F) -> T
    where
        F: FnOnce(Arc<Model>) -> T,
    {
        match self.model.upgrade() {
            Some(model) => f(model),
            None => panic!(
                "Model does not exist anymore. Parent model is deleted without deleting the child fields."
            )
        }
    }

    pub fn model_id_column(&self) -> Column {
        self.with_model(|model| {
            model.with_project(|project| {
                let db_name = project.db_name();
                let table_name = model.db_name();
                let id_field = model.fields().id();
                let id_name = id_field.db_name();

                (db_name, table_name, id_name).into()
            })
        })
    }
}
