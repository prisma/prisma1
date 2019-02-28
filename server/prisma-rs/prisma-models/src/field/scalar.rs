use prisma_query::ast::*;

use super::FieldManifestation;
use crate::prelude::*;
use std::sync::Arc;

static ID_FIELD: &str = "id";
static EMBEDDED_ID_FIELD: &str = "_id";
static UPDATED_AT_FIELD: &str = "updatedAt";
static CREATED_AT_FIELD: &str = "createdAt";

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScalarFieldTemplate {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub manifestation: Option<FieldManifestation>,
    pub behaviour: Option<FieldBehaviour>,
}

#[derive(DebugStub)]
pub struct ScalarField {
    pub name: String,
    pub type_identifier: TypeIdentifier,
    pub is_required: bool,
    pub is_list: bool,
    pub is_unique: bool,
    pub is_hidden: bool,
    pub is_readonly: bool,
    pub is_auto_generated: bool,
    pub manifestation: Option<FieldManifestation>,
    pub behaviour: Option<FieldBehaviour>,
    #[debug_stub = "#ModelWeakRef#"]
    pub model: ModelWeakRef,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", tag = "type")]
pub enum FieldBehaviour {
    CreatedAt,
    UpdatedAt,
    Id {
        strategy: IdStrategy,
        sequence: Option<Sequence>,
    },
    ScalarList {
        strategy: ScalarListStrategy,
    },
}

#[derive(Debug, Deserialize, Clone, Copy)]
pub enum IdStrategy {
    Auto,
    None,
    Sequence,
}

#[derive(Debug, Deserialize, Clone, Copy)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Sequence {
    pub name: String,
    pub initial_value: i32,
    pub allocation_size: i32,
}

impl ScalarField {
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

    pub fn with_project<F, T>(&self, f: F) -> T
    where
        F: FnOnce(Arc<Project>) -> T,
    {
        self.with_model(|m| m.with_project(|p| f(p)))
    }

    /// A field is an ID field if the name is `id` or `_id` in legacy schemas,
    /// or if the field has Id behaviour defined.
    pub fn is_id(&self) -> bool {
        self.with_model(|model| {
            if model.is_legacy() {
                self.name == ID_FIELD || self.name == EMBEDDED_ID_FIELD
            } else {
                match self.behaviour {
                    Some(FieldBehaviour::Id { .. }) => true,
                    _ => false,
                }
            }
        })
    }

    pub fn is_created_at(&self) -> bool {
        self.with_model(|model| {
            if model.is_legacy() {
                self.name == CREATED_AT_FIELD
            } else {
                match self.behaviour {
                    Some(FieldBehaviour::CreatedAt) => true,
                    _ => false,
                }
            }
        })
    }

    pub fn is_updated_at(&self) -> bool {
        self.with_model(|model| {
            if model.is_legacy() {
                self.name == UPDATED_AT_FIELD
            } else {
                match self.behaviour {
                    Some(FieldBehaviour::UpdatedAt) => true,
                    _ => false,
                }
            }
        })
    }

    pub fn is_writable(&self) -> bool {
        !self.is_readonly && !self.is_id() && !self.is_created_at() && !self.is_updated_at()
    }

    pub fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }

    pub fn model_column(&self) -> Column {
        self.with_model(|model| {
            model
                .with_project(|project| (project.db_name(), model.db_name(), self.db_name()).into())
        })
    }
}
