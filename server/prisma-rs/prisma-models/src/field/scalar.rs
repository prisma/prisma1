use prisma_query::ast::*;

use super::FieldManifestation;
use crate::prelude::*;

static ID_FIELD: &str = "id";
static EMBEDDED_ID_FIELD: &str = "_id";
static UPDATED_AT_FIELD: &str = "updatedAt";
static CREATED_AT_FIELD: &str = "createdAt";

#[derive(Debug, Deserialize, Serialize)]
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

#[derive(Debug, Deserialize, Serialize, Clone, Eq, PartialEq)]
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

#[derive(Debug, Deserialize, Serialize, Clone, Copy, Eq, PartialEq)]
pub enum IdStrategy {
    Auto,
    None,
    Sequence,
}

#[derive(Debug, Deserialize, Serialize, Clone, Copy, Eq, PartialEq)]
pub enum ScalarListStrategy {
    Embedded,
    Relation,
}

#[derive(Debug, Deserialize, Serialize, Clone, Eq, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Sequence {
    pub name: String,
    pub initial_value: i32,
    pub allocation_size: i32,
}

impl ScalarField {
    pub fn model(&self) -> ModelRef {
        self.model
            .upgrade()
            .expect("Model does not exist anymore. Parent model got deleted without deleting the child.")
    }

    pub fn schema(&self) -> SchemaRef {
        self.model().schema()
    }

    /// A field is an ID field if the name is `id` or `_id` in legacy schemas,
    /// or if the field has Id behaviour defined.
    pub fn is_id(&self) -> bool {
        if self.model().is_legacy() {
            self.name == ID_FIELD || self.name == EMBEDDED_ID_FIELD
        } else {
            match self.behaviour {
                Some(FieldBehaviour::Id { .. }) => true,
                _ => false,
            }
        }
    }

    pub fn is_created_at(&self) -> bool {
        if self.model().is_legacy() {
            self.name == CREATED_AT_FIELD
        } else {
            match self.behaviour {
                Some(FieldBehaviour::CreatedAt) => true,
                _ => false,
            }
        }
    }

    pub fn is_updated_at(&self) -> bool {
        if self.model().is_legacy() {
            self.name == UPDATED_AT_FIELD
        } else {
            match self.behaviour {
                Some(FieldBehaviour::UpdatedAt) => true,
                _ => false,
            }
        }
    }

    pub fn is_writable(&self) -> bool {
        !self.is_readonly && !self.is_id() && !self.is_created_at() && !self.is_updated_at()
    }

    pub fn db_name(&self) -> &str {
        self.db_name_opt().unwrap_or_else(|| self.name.as_ref())
    }

    pub fn db_name_opt(&self) -> Option<&str> {
        self.manifestation.as_ref().map(|mf| mf.db_name.as_ref())
    }

    pub fn as_column(&self) -> Column {
        (self.schema().db_name.as_str(), self.model().db_name(), self.db_name()).into()
    }

    pub fn id_behaviour_clone(&self) -> Option<FieldBehaviour> {
        if self.is_id() {
            self.behaviour.clone()
        } else {
            None
        }
    }

    pub fn scalar_list_behaviour_clone(&self) -> Option<FieldBehaviour> {
        match self.behaviour {
            Some(ref b) => match b {
                FieldBehaviour::ScalarList { strategy } => Some(FieldBehaviour::ScalarList { strategy: *strategy }),
                _ => None,
            },
            _ => None,
        }
    }
}
