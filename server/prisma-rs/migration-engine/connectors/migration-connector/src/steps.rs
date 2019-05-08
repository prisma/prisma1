use nullable::Nullable;
use datamodel::*;

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
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
    CreateRelation(CreateRelation),
    DeleteRelation(DeleteRelation),
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateModel {
    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub db_name: Option<String>,

    pub embedded: bool,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
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

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeleteModel {
    pub name: String,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq, Default)]
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

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateField {
    pub model: String,

    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_name: Option<String>,

    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub tpe: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub db_name: Option<Nullable<String>>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_optional: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_list: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_created_at: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_updated_at: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<Nullable<String>>, // fixme: change to behaviour

    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Nullable<String>>, // fixme: change to PrismaValue

    #[serde(skip_serializing_if = "Option::is_none")]
    pub scalar_list: Option<Nullable<String>>, // fixme: change to behaviour
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeleteField {
    pub model: String,
    pub name: String,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateEnum {
    pub name: String,
    pub values: Vec<String>,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateEnum {
    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_name: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub values: Option<Vec<String>>,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeleteEnum {
    pub name: String,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CreateRelation {
    pub name: String,
    pub model_a: RelationFieldSpec,
    pub model_b: RelationFieldSpec,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub table: Option<LinkTableSpec>,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UpdateRelation {
    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_name: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_a: Option<RelationFieldSpec>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_b: Option<RelationFieldSpec>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub table: Option<LinkTableSpec>,
}

#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeleteRelation {
    pub name: String,
}

// fixme: this data structure is used in create and update. It does not allow to set field to null though in update.
// fixme: the field inline_link does not allow to customize the underlying db name right now.
#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RelationFieldSpec {
    pub name: String,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub field: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_list: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_optional: Option<bool>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub on_delete: Option<String>, // fixme: change to proper enum

    #[serde(skip_serializing_if = "Option::is_none")]
    pub inline_link: Option<bool>,
}

// fixme: this strucut does not allow to customize the db name of the link table.
#[derive(Debug, Deserialize, Serialize, Eq, PartialEq)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LinkTableSpec {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_a_column: Option<String>,

    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_b_column: Option<String>,
}

#[cfg(test)]
#[allow(non_snake_case)]
mod tests {
    use crate::steps::*;
    use nullable::Nullable::*;
    // use prisma_models::prelude::IdStrategy;
    // use prisma_models::prelude::ScalarListStrategy;
    // use prisma_models::Field;
    // use prisma_models::FieldBehaviour;
    // use prisma_models::OnDelete;
    // use prisma_models::Sequence;
    use serde_json::Value;

    #[test]
    fn minimal_CreateModel_must_work() {
        let json = r#"{"stepType":"CreateModel","name":"Blog","embedded":false}"#;
        let expected_struct = MigrationStep::CreateModel(CreateModel {
            name: "Blog".to_string(),
            db_name: None,
            embedded: false,
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn full_CreateModel_must_work() {
        let json = r#"{"stepType":"CreateModel","name":"Blog","dbName":"blog","embedded":true}"#;
        let expected_struct = MigrationStep::CreateModel(CreateModel {
            name: "Blog".to_string(),
            db_name: Some("blog".to_string()),
            embedded: true,
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn minimal_UpdateModel_must_work() {
        let json = r#"{"stepType":"UpdateModel","name":"Blog"}"#;
        let expected_struct = MigrationStep::UpdateModel(UpdateModel {
            name: "Blog".to_string(),
            new_name: None,
            db_name: None,
            embedded: None,
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn full_UpdateModel_must_work() {
        let json = r#"{"stepType":"UpdateModel","name":"Blog","newName":"MyBlog","dbName":"blog","embedded":true}"#;
        let expected_struct = MigrationStep::UpdateModel(UpdateModel {
            name: "Blog".to_string(),
            new_name: Some("MyBlog".to_string()),
            db_name: Some(NotNull("blog".to_string())),
            embedded: Some(true),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn DeleteModel_must_work() {
        let json = r#"{"stepType":"DeleteModel","name":"Blog"}"#;
        let expected_struct = MigrationStep::DeleteModel(DeleteModel {
            name: "Blog".to_string(),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn minimal_CreateField_must_work() {
        let json = r#"{"stepType":"CreateField","model":"Blog","name":"title","type":"String"}"#;
        let expected_struct = MigrationStep::CreateField(CreateField {
            model: "Blog".to_string(),
            name: "title".to_string(),
            tpe: "String".to_string(),
            db_name: None,
            is_optional: None,
            is_list: None,
            is_created_at: None,
            is_updated_at: None,
            id: None,
            default: None,
            scalar_list: None,
        });
        assert_symmetric_serde(json, expected_struct);
    }

    // TODO: bring back once we have decided on field behavious
    //#[test]
    // fn full_CreateField_must_work() {
    //     let json = r#"{
    //         "stepType":"CreateField",
    //         "model":"Blog",
    //         "name":"title",
    //         "type":"String",
    //         "dbName":"blog",
    //         "isOptional":true,
    //         "isList":true,
    //         "isCreatedAt":true,
    //         "isUpdatedAt":true,
    //         "id": {
    //             "type": "id",
    //             "strategy":"Sequence",
    //             "sequence": {
    //                 "name": "My_Sequence",
    //                 "allocationSize": 5,
    //                 "initialValue": 100
    //             }
    //         },
    //         "default":"default",
    //         "scalarList": {
    //             "type":"scalarList",
    //             "strategy": "Embedded"
    //         }
    //     }"#;
    //     let sequence = Sequence {
    //         name: "My_Sequence".to_string(),
    //         allocation_size: 5,
    //         initial_value: 100,
    //     };
    //     let expected_struct = MigrationStep::CreateField(CreateField {
    //         model: "Blog".to_string(),
    //         name: "title".to_string(),
    //         tpe: "String".to_string(),
    //         db_name: Some("blog".to_string()),
    //         is_optional: Some(true),
    //         is_list: Some(true),
    //         is_created_at: Some(true),
    //         is_updated_at: Some(true),
    //         id: Some(FieldBehaviour::Id {
    //             strategy: IdStrategy::Sequence,
    //             sequence: Some(sequence),
    //         }),
    //         default: Some("default".to_string()),
    //         scalar_list: Some(FieldBehaviour::ScalarList {
    //             strategy: ScalarListStrategy::Embedded,
    //         }),
    //     });
    //     assert_symmetric_serde(json, expected_struct);
    // }

    #[test]
    fn minimal_UpdateField_must_work() {
        let json = r#"{"stepType":"UpdateField","model":"Blog","name":"title"}"#;
        let expected_struct = MigrationStep::UpdateField(UpdateField {
            model: "Blog".to_string(),
            name: "title".to_string(),
            new_name: None,
            tpe: None,
            db_name: None,
            is_optional: None,
            is_list: None,
            is_created_at: None,
            is_updated_at: None,
            id: None,
            default: None,
            scalar_list: None,
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn full_UpdateField_must_work() {
        let json = r#"{"stepType":"UpdateField","model":"Blog","name":"title","newName":"MyBlog","type":"String","dbName":"blog","isOptional":true,"isList":true,"isCreatedAt":true,"isUpdatedAt":true,"id":"id","default":"default","scalarList":"scalarList"}"#;
        let expected_struct = MigrationStep::UpdateField(UpdateField {
            model: "Blog".to_string(),
            name: "title".to_string(),
            new_name: Some("MyBlog".to_string()),
            tpe: Some("String".to_string()),
            db_name: Some(NotNull("blog".to_string())),
            is_optional: Some(true),
            is_list: Some(true),
            is_created_at: Some(true),
            is_updated_at: Some(true),
            id: Some(NotNull("id".to_string())),
            default: Some(NotNull("default".to_string())),
            scalar_list: Some(NotNull("scalarList".to_string())),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn DeleteField_must_work() {
        let json = r#"{"stepType":"DeleteField","model":"Blog","name":"title"}"#;
        let expected_struct = MigrationStep::DeleteField(DeleteField {
            model: "Blog".to_string(),
            name: "title".to_string(),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn CreateEnum_must_work() {
        let json = r#"{"stepType":"CreateEnum","name":"BlogCategory","values":["Politics","Tech"]}"#;
        let expected_struct = MigrationStep::CreateEnum(CreateEnum {
            name: "BlogCategory".to_string(),
            values: vec!["Politics".to_string(), "Tech".to_string()],
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn minimal_UpdateEnum_must_work() {
        let json = r#"{"stepType":"UpdateEnum","name":"BlogCategory"}"#;
        let expected_struct = MigrationStep::UpdateEnum(UpdateEnum {
            name: "BlogCategory".to_string(),
            new_name: None,
            values: None,
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn full_Update_Enum_must_work() {
        let json = r#"{"stepType":"UpdateEnum","name":"BlogCategory","newName":"MyBlogCategory","values":["Tech"]}"#;
        let expected_struct = MigrationStep::UpdateEnum(UpdateEnum {
            name: "BlogCategory".to_string(),
            new_name: Some("MyBlogCategory".to_string()),
            values: Some(vec!["Tech".to_string()]),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn DeleteEnum_must_work() {
        let json = r#"{"stepType":"DeleteEnum","name":"BlogCategory"}"#;
        let expected_struct = MigrationStep::DeleteEnum(DeleteEnum {
            name: "BlogCategory".to_string(),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn minimal_CreateRelation_must_work() {
        let json = r#"{
            "stepType":"CreateRelation",
            "name":"BlogToPosts",
            "modelA": { "name":"Blog" },
            "modelB": { "name":"Post" }
        }"#;
        let expected_struct = MigrationStep::CreateRelation(CreateRelation {
            name: "BlogToPosts".to_string(),
            model_a: RelationFieldSpec {
                name: "Blog".to_string(),
                field: None,
                is_list: None,
                is_optional: None,
                on_delete: None,
                inline_link: None,
            },
            model_b: RelationFieldSpec {
                name: "Post".to_string(),
                field: None,
                is_list: None,
                is_optional: None,
                on_delete: None,
                inline_link: None,
            },
            table: None,
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn full_CreateRelation_with_link_table_must_work() {
        let json = r#"{
            "stepType":"CreateRelation",
            "name":"BlogToPosts",
            "modelA": { "name":"Blog","field":"posts","isList":true,"onDelete":"SET_NULL","inlineLink":true},
            "modelB": { "name":"Post","field":"blog","isOptional":true,"onDelete":"CASCADE"},
            "table": { "modelAColumn":"blog", "modelBColumn":"post" }
        }"#;
        let expected_struct = MigrationStep::CreateRelation(CreateRelation {
            name: "BlogToPosts".to_string(),
            model_a: RelationFieldSpec {
                name: "Blog".to_string(),
                field: Some("posts".to_string()),
                is_list: Some(true),
                is_optional: None,
                on_delete: Some("SET_NULL".to_string()),
                inline_link: Some(true),
            },
            model_b: RelationFieldSpec {
                name: "Post".to_string(),
                field: Some("blog".to_string()),
                is_list: None,
                is_optional: Some(true),
                on_delete: Some("CASCADE".to_string()),
                inline_link: None,
            },
            table: Some(LinkTableSpec {
                model_a_column: Some("blog".to_string()),
                model_b_column: Some("post".to_string()),
            }),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn CreateRelation_forcing_the_link_table_must_work() {
        let json = r#"{
            "stepType":"CreateRelation",
            "name":"BlogToPosts",
            "modelA": { "name":"Blog" },
            "modelB": { "name":"Post" },
            "table": { }
        }"#;
        let expected_struct = MigrationStep::CreateRelation(CreateRelation {
            name: "BlogToPosts".to_string(),
            model_a: RelationFieldSpec {
                name: "Blog".to_string(),
                field: None,
                is_list: None,
                is_optional: None,
                on_delete: None,
                inline_link: None,
            },
            model_b: RelationFieldSpec {
                name: "Post".to_string(),
                field: None,
                is_list: None,
                is_optional: None,
                on_delete: None,
                inline_link: None,
            },
            table: Some(LinkTableSpec {
                model_a_column: None,
                model_b_column: None,
            }),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    #[test]
    fn DeletRelation_must_work() {
        let json = r#"{"stepType":"DeleteRelation","name":"BlogToPost"}"#;
        let expected_struct = MigrationStep::DeleteRelation(DeleteRelation {
            name: "BlogToPost".to_string(),
        });
        assert_symmetric_serde(json, expected_struct);
    }

    fn assert_symmetric_serde(json: &str, expected: MigrationStep) {
        let serde_value: Value = serde_json::from_str(&json).expect("The provided input was invalid json.");
        let deserialized: MigrationStep = serde_json::from_str(&json).expect("Deserialization failed.");
        let serialized_again = serde_json::to_value(&deserialized).expect("Serialization failed");
        assert_eq!(
            deserialized, expected,
            "The provided json could not be serialized into the expected struct."
        );
        assert_eq!(
            serialized_again, serde_value,
            "Reserializing did not produce the original json input."
        );
    }
}
