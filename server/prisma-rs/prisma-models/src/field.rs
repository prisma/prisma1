mod relation;
mod scalar;

pub use relation::*;
pub use scalar::*;

use crate::prelude::*;
use once_cell::sync::OnceCell;
use prisma_query::ast::Column;
use std::{borrow::Cow, sync::Arc};

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", untagged)]
pub enum FieldTemplate {
    Relation(RelationFieldTemplate),
    Scalar(ScalarFieldTemplate),
}

#[derive(Debug)]
pub enum Field {
    Relation(RelationFieldRef),
    Scalar(ScalarFieldRef),
}

#[derive(Debug, Deserialize, Serialize, PartialEq)]
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
    pub fn user_friendly_type_name(self) -> String {
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

    pub fn name(&self) -> &str {
        match self {
            Field::Scalar(ref sf) => &sf.name,
            Field::Relation(ref rf) => &rf.name,
        }
    }

    pub fn as_column(&self) -> Column {
        match self {
            Field::Scalar(ref sf) => sf.as_column(),
            Field::Relation(ref rf) => rf.as_column(),
        }
    }

    pub fn is_visible(&self) -> bool {
        match self {
            Field::Scalar(ref sf) => !sf.is_hidden,
            Field::Relation(ref rf) => !rf.is_hidden,
        }
    }

    pub fn is_scalar(&self) -> bool {
        match self {
            Field::Scalar(_) => true,
            Field::Relation(_) => false,
        }
    }

    pub fn is_list(&self) -> bool {
        match self {
            Field::Scalar(ref sf) => sf.is_list,
            Field::Relation(ref rf) => rf.is_list,
        }
    }

    pub fn is_required(&self) -> bool {
        match self {
            Field::Scalar(ref sf) => sf.is_required,
            Field::Relation(ref rf) => rf.is_required,
        }
    }

    pub fn type_identifier(&self) -> TypeIdentifier {
        match self {
            Field::Scalar(ref sf) => sf.type_identifier,
            Field::Relation(ref rf) => rf.type_identifier,
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
                    is_hidden: st.is_hidden,
                    is_auto_generated: st.is_auto_generated,
                    is_unique: st.is_unique,
                    manifestation: st.manifestation,
                    internal_enum: st.internal_enum,
                    behaviour: st.behaviour,
                    model,
                    default_value: st.default_value,
                };

                Field::Scalar(Arc::new(scalar))
            }
            FieldTemplate::Relation(rt) => {
                let relation = RelationField {
                    name: rt.name,
                    type_identifier: rt.type_identifier,
                    is_required: rt.is_required,
                    is_list: rt.is_list,
                    is_hidden: rt.is_hidden,
                    is_auto_generated: rt.is_auto_generated,
                    is_unique: rt.is_unique,
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
