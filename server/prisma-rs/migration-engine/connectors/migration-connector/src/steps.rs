use datamodel::*;
use nullable::Nullable;

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(tag = "stepType")]
pub enum MigrationStep {
    CreateModel(CreateModel),
    UpdateModel(UpdateModel),
    DeleteModel(DeleteModel),
    CreateField(CreateField),
    DeleteField(DeleteField),
    UpdateField(UpdateField),
    CreateEnum(CreateEnum),
    UpdateEnum(UpdateEnum),
    DeleteEnum(DeleteEnum),
    // CreateRelation(CreateRelation),
    // DeleteRelation(DeleteRelation),
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Eq, Hash, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateModel {
    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub db_name: Option<String>,

    pub embedded: bool,
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Eq, Hash, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateModel {
    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_name: Option<String>,

    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        deserialize_with = "nullable::optional_nullable_deserialize"
    )]
    pub db_name: Option<Nullable<String>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub embedded: Option<bool>,
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeleteModel {
    pub name: String,
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateField {
    pub model: String,

    pub name: String,

    #[serde(rename = "type")]
    pub tpe: FieldType,

    pub arity: FieldArity,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub db_name: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_created_at: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_updated_at: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<String>, // fixme: change to behaviour

    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Value>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub scalar_list: Option<ScalarListStrategy>,
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateField {
    pub model: String,

    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_name: Option<String>,

    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub tpe: Option<FieldType>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub arity: Option<FieldArity>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub db_name: Option<Nullable<String>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_created_at: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_updated_at: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Nullable<String>>, // fixme: change to behaviour

    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Nullable<Value>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub scalar_list: Option<Nullable<ScalarListStrategy>>,
}

impl UpdateField {
    pub fn is_any_option_set(&self) -> bool {
        self.new_name.is_some()
            || self.arity.is_some()
            || self.db_name.is_some()
            || self.is_created_at.is_some()
            || self.is_updated_at.is_some()
            || self.id.is_some()
            || self.default.is_some()
            || self.scalar_list.is_some()
    }
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeleteField {
    pub model: String,
    pub name: String,
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateEnum {
    pub name: String,
    pub values: Vec<String>,
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateEnum {
    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_name: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub values: Option<Vec<String>>,
}

#[derive(Debug, Deserialize, Serialize, PartialEq, Clone)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeleteEnum {
    pub name: String,
}

// #[derive(Debug, Deserialize, Serialize, PartialEq)]
// #[serde(rename_all = "camelCase", deny_unknown_fields)]
// pub struct CreateRelation {
//     pub name: String,
//     pub model_a: RelationFieldSpec,
//     pub model_b: RelationFieldSpec,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub table: Option<LinkTableSpec>,
// }

// #[derive(Debug, Deserialize, Serialize, PartialEq)]
// #[serde(rename_all = "camelCase", deny_unknown_fields)]
// pub struct UpdateRelation {
//     pub name: String,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub new_name: Option<String>,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub model_a: Option<RelationFieldSpec>,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub model_b: Option<RelationFieldSpec>,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub table: Option<LinkTableSpec>,
// }

// #[derive(Debug, Deserialize, Serialize, PartialEq)]
// #[serde(rename_all = "camelCase", deny_unknown_fields)]
// pub struct DeleteRelation {
//     pub name: String,
// }

// // fixme: this data structure is used in create and update. It does not allow to set field to null though in update.
// // fixme: the field inline_link does not allow to customize the underlying db name right now.
// #[derive(Debug, Deserialize, Serialize, PartialEq)]
// #[serde(rename_all = "camelCase", deny_unknown_fields)]
// pub struct RelationFieldSpec {
//     pub name: String,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub field: Option<String>,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub is_list: Option<bool>,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub is_optional: Option<bool>,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub on_delete: Option<String>, // fixme: change to proper enum

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub inline_link: Option<bool>,
// }

// // fixme: this strucut does not allow to customize the db name of the link table.
// #[derive(Debug, Deserialize, Serialize, PartialEq)]
// #[serde(rename_all = "camelCase", deny_unknown_fields)]
// pub struct LinkTableSpec {
//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub model_a_column: Option<String>,

//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub model_b_column: Option<String>,
// }
