use crate::{
    error::Error,
    models::{Model, ModelWeakRef, Renameable},
    PrismaResult,
};

use std::{collections::BTreeSet, rc::Rc};

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
#[serde(rename_all = "camelCase")]
pub struct FieldManifestation {
    pub db_name: String,
}

#[derive(Debug)]
pub struct Fields {
    pub all: Vec<Field>,
}

impl Fields {
    pub fn new(all: Vec<Field>) -> Fields {
        Fields { all }
    }

    pub fn scalar(&self) -> Vec<&ScalarField> {
        self.all.iter().fold(Vec::new(), Self::scalar_filter)
    }

    pub fn find_many_from_all(&self, names: &BTreeSet<&str>) -> Vec<&Field> {
        self.all
            .iter()
            .filter(|field| names.contains(field.db_name()))
            .collect()
    }

    pub fn find_many_from_scalar(&self, names: &BTreeSet<&str>) -> Vec<&ScalarField> {
        self.all
            .iter()
            .filter(|field| names.contains(field.db_name()))
            .fold(Vec::new(), Self::scalar_filter)
    }

    pub fn find_many_from_relation(&self, names: &BTreeSet<&str>) -> Vec<&RelationField> {
        self.all
            .iter()
            .filter(|field| names.contains(field.db_name()))
            .fold(Vec::new(), Self::relation_filter)
    }

    pub fn find_from_all(&self, name: &str) -> PrismaResult<&Field> {
        self.all
            .iter()
            .find(|field| field.db_name() == name)
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    pub fn find_from_scalar(&self, name: &str) -> PrismaResult<&ScalarField> {
        self.all
            .iter()
            .find(|field| field.db_name() == name)
            .and_then(|field| {
                if let Field::Scalar(scalar_field) = field {
                    Some(scalar_field)
                } else {
                    None
                }
            })
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    pub fn find_from_relation(&self, name: &str) -> PrismaResult<&RelationField> {
        self.all
            .iter()
            .find(|field| field.db_name() == name)
            .and_then(|field| {
                if let Field::Relation(relation_field) = field {
                    Some(relation_field)
                } else {
                    None
                }
            })
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    fn scalar_filter<'a>(mut acc: Vec<&'a ScalarField>, field: &'a Field) -> Vec<&'a ScalarField> {
        if let Field::Scalar(scalar_field) = field {
            acc.push(scalar_field);
        };

        acc
    }

    fn relation_filter<'a>(
        mut acc: Vec<&'a RelationField>,
        field: &'a Field,
    ) -> Vec<&'a RelationField> {
        if let Field::Relation(relation_field) = field {
            acc.push(relation_field);
        };

        acc
    }
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
    pub fn opposite(self) -> RelationSide {
        match self {
            RelationSide::A => RelationSide::B,
            RelationSide::B => RelationSide::A,
        }
    }
}

impl ScalarField {
    fn with_model<F, T>(&self, f: F) -> T
    where
        F: FnOnce(Rc<Model>) -> T,
    {
        match self.model.upgrade() {
            Some(model) => f(model),
            None => panic!(
                "Model does not exist anymore. Parent model is deleted without deleting the child fields."
            )
        }
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
}

impl Renameable for ScalarField {
    fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}

impl Renameable for RelationField {
    fn db_name(&self) -> &str {
        self.manifestation
            .as_ref()
            .map(|mf| mf.db_name.as_ref())
            .unwrap_or_else(|| self.name.as_ref())
    }
}

impl Renameable for Field {
    fn db_name(&self) -> &str {
        match self {
            Field::Scalar(sf) => sf.db_name(),
            Field::Relation(rf) => rf.db_name(),
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
                    model,
                };

                Field::Relation(relation)
            }
        }
    }
}
