mod relation;
mod scalar;

pub use relation::*;
pub use scalar::*;

use crate::prelude::*;
use once_cell::unsync::OnceCell;
use std::{borrow::Cow, sync::Arc};

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", untagged)]
pub enum FieldTemplate {
    Relation(RelationFieldTemplate),
    Scalar(ScalarFieldTemplate),
}

#[derive(Debug)]
pub enum Field {
    Relation(Arc<RelationField>),
    Scalar(Arc<ScalarField>),
}

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FieldManifestation {
    pub db_name: String,
}

#[derive(Clone, Copy, Debug, Deserialize, Serialize, PartialEq)]
pub enum TypeIdentifier {
    String,
    Float,
    Boolean,
    Enum,
    Json,
    DateTime,
    GraphQLID,
    UUID,
    Int,
    Relation,
}

impl TypeIdentifier {
    pub fn user_friendly_type_name(&self) -> String {
        match self {
            TypeIdentifier::GraphQLID => "ID".to_string(),
            _ => format!("{:?}", self),
        }
    }
}

impl Field {
    pub fn db_name(&self) -> Cow<str> {
        match self {
            Field::Scalar(ref sf) => Cow::from(sf.db_name()),
            Field::Relation(ref rf) => Cow::from(rf.db_name()),
        }
    }
}

impl FieldTemplate {
    pub fn build(self, model: ModelWeakRef) -> Field {
        match self {
            FieldTemplate::Scalar(st) => {
                let scalar = ScalarField {
                    name: st.name,
                    type_identifier: st.type_identifier,
                    is_required: st.is_required,
                    is_list: st.is_list,
                    is_unique: st.is_unique,
                    is_hidden: st.is_hidden,
                    is_readonly: st.is_readonly,
                    is_auto_generated: st.is_auto_generated,
                    manifestation: st.manifestation,
                    behaviour: st.behaviour,
                    model,
                };

                Field::Scalar(Arc::new(scalar))
            }
            FieldTemplate::Relation(rt) => {
                let relation = RelationField {
                    name: rt.name,
                    type_identifier: rt.type_identifier,
                    is_required: rt.is_required,
                    is_list: rt.is_list,
                    is_unique: rt.is_unique,
                    is_hidden: rt.is_hidden,
                    is_readonly: rt.is_readonly,
                    is_auto_generated: rt.is_auto_generated,
                    relation_name: rt.relation_name,
                    relation_side: rt.relation_side,
                    model,
                    relation: OnceCell::new(),
                };

                Field::Relation(Arc::new(relation))
            }
        }
    }
}
