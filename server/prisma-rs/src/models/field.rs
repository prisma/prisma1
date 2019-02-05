use crate::{
    models::{
        ModelRef,
        Renameable,
    },
};

static ID_FIELD: &str = "id";
static EMBEDDED_ID_FIELD: &str = "_id";
static UPDATED_AT_FIELD: &str = "updatedAt";
static CREATED_AT_FIELD: &str = "createdAt";

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase", untagged)]
pub enum FieldTemplate {
    Scalar(ScalarFieldTemplate),
    Relation(RelationFieldTemplate),
}

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

#[derive(Debug)]
pub enum Field {
    Scalar(ScalarField),
    Relation(RelationField),
}

#[derive(Debug)]
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
    pub model: ModelRef,
}


#[derive(Debug)]
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
    pub model: ModelRef,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FieldManifestation {
    pub db_name: String,
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

#[derive(Clone, Copy, Debug, Deserialize, PartialEq)]
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

#[derive(Clone, Copy, Debug, Deserialize, PartialEq)]
pub enum RelationSide {
    A,
    B,
}

impl RelationSide {
    pub fn opposite(&self) -> RelationSide {
        match self {
            RelationSide::A => RelationSide::B,
            RelationSide::B => RelationSide::A,
        }
    }
}

impl ScalarField {
    pub fn is_id(&self) -> bool {
        if self.model.borrow().is_legacy() {
            self.name == ID_FIELD || self.name == EMBEDDED_ID_FIELD
        } else {
            unimplemented!()
        }
    }

    pub fn is_created_at(&self) -> bool {
        if self.model.borrow().is_legacy() {
            self.name == CREATED_AT_FIELD
        } else {
            unimplemented!()
        }
    }

    pub fn is_updated_at(&self) -> bool {
        if self.model.borrow().is_legacy() {
            self.name == UPDATED_AT_FIELD
        } else {
            unimplemented!()
        }
    }

    pub fn is_writable(&self) -> bool {
        !self.is_readonly && !self.is_id() && !self.is_created_at() && !self.is_updated_at()
    }
}

impl Renameable for ScalarField {
    fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}

impl FieldTemplate {
    pub fn build(self, model: ModelRef) -> Field {
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
                    model: model,
                };

                Field::Scalar(scalar)
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
                    manifestation: rt.manifestation,
                    relation_name: rt.relation_name,
                    relation_side: rt.relation_side,
                    model: model,
                };

                Field::Relation(relation)
            }
        }
    }
}
