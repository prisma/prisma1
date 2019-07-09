#![allow(non_snake_case)]

use datamodel::*;
use migration_connector::steps::*;

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
        db_name: Some(Some("blog".to_string())),
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
    let json =
        r#"{"stepType":"CreateField","model":"Blog","name":"title","type":{"Base":"String"},"arity":"required","isUnique":false}"#;
    let expected_struct = MigrationStep::CreateField(CreateField {
        model: "Blog".to_string(),
        name: "title".to_string(),
        tpe: FieldType::Base(ScalarType::String),
        arity: FieldArity::Required,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: false,
        id: None,
        default: None,
        scalar_list: None,
    });
    assert_symmetric_serde(json, expected_struct);
}

// TODO: bring back once we have decided on field behavious
#[test]
fn full_CreateField_must_work() {
    let json = r#"{
            "stepType":"CreateField",
            "model":"Blog",
            "name":"title",
            "type":{"Base":"String"},
            "arity":"optional",
            "dbName":"blog",
            "isCreatedAt":true,
            "isUpdatedAt":true,
            "isUnique": true,
            "default":{"String":"default"},
            "scalarList": "Embedded"
        }"#;
    let expected_struct = MigrationStep::CreateField(CreateField {
        model: "Blog".to_string(),
        name: "title".to_string(),
        tpe: FieldType::Base(ScalarType::String),
        arity: FieldArity::Optional,
        db_name: Some("blog".to_string()),
        is_created_at: Some(true),
        is_updated_at: Some(true),
        is_unique: true,
        id: None, // TODO: adapt once added to CreateField
        default: Some(Value::String("default".to_string())),
        scalar_list: Some(ScalarListStrategy::Embedded),
    });

    assert_symmetric_serde(json, expected_struct);
}

#[test]
fn minimal_UpdateField_must_work() {
    let json = r#"{"stepType":"UpdateField","model":"Blog","name":"title"}"#;
    let expected_struct = MigrationStep::UpdateField(UpdateField {
        model: "Blog".to_string(),
        name: "title".to_string(),
        new_name: None,
        tpe: None,
        arity: None,
        db_name: None,
        is_created_at: None,
        is_updated_at: None,
        is_unique: None,
        id_info: None,
        default: None,
        scalar_list: None,
    });
    assert_symmetric_serde(json, expected_struct);
}

#[test]
fn full_UpdateField_must_work() {
    let json = r#"{"stepType":"UpdateField","model":"Blog","name":"title","newName":"MyBlog","type":{"Base":"String"},"arity":"optional","dbName":"blog","isCreatedAt":true,"isUpdatedAt":true,"isUnique":true,"default":{"String":"default"},"scalarList":"Embedded"}"#;
    let expected_struct = MigrationStep::UpdateField(UpdateField {
        model: "Blog".to_string(),
        name: "title".to_string(),
        new_name: Some("MyBlog".to_string()),
        tpe: Some(FieldType::Base(ScalarType::String)),
        arity: Some(FieldArity::Optional),
        db_name: Some(Some("blog".to_string())),
        is_created_at: Some(true),
        is_updated_at: Some(true),
        is_unique: Some(true),
        id_info: None,
        default: Some(Some(Value::String("default".to_string()))),
        scalar_list: Some(Some(ScalarListStrategy::Embedded)),
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

// #[test]
// fn CreateEnum_must_work() {
//     let json = r#"{"stepType":"CreateEnum","name":"BlogCategory","values":["Politics","Tech"]}"#;
//     let expected_struct = MigrationStep::CreateEnum(CreateEnum {
//         name: "BlogCategory".to_string(),
//         values: vec!["Politics".to_string(), "Tech".to_string()],
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

// #[test]
// fn minimal_UpdateEnum_must_work() {
//     let json = r#"{"stepType":"UpdateEnum","name":"BlogCategory"}"#;
//     let expected_struct = MigrationStep::UpdateEnum(UpdateEnum {
//         name: "BlogCategory".to_string(),
//         new_name: None,
//         values: None,
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

// #[test]
// fn full_Update_Enum_must_work() {
//     let json = r#"{"stepType":"UpdateEnum","name":"BlogCategory","newName":"MyBlogCategory","values":["Tech"]}"#;
//     let expected_struct = MigrationStep::UpdateEnum(UpdateEnum {
//         name: "BlogCategory".to_string(),
//         new_name: Some("MyBlogCategory".to_string()),
//         values: Some(vec!["Tech".to_string()]),
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

// #[test]
// fn DeleteEnum_must_work() {
//     let json = r#"{"stepType":"DeleteEnum","name":"BlogCategory"}"#;
//     let expected_struct = MigrationStep::DeleteEnum(DeleteEnum {
//         name: "BlogCategory".to_string(),
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

// #[test]
// fn minimal_CreateRelation_must_work() {
//     let json = r#"{
//         "stepType":"CreateRelation",
//         "name":"BlogToPosts",
//         "modelA": { "name":"Blog" },
//         "modelB": { "name":"Post" }
//     }"#;
//     let expected_struct = MigrationStep::CreateRelation(CreateRelation {
//         name: "BlogToPosts".to_string(),
//         model_a: RelationFieldSpec {
//             name: "Blog".to_string(),
//             field: None,
//             is_list: false,
//             is_optional: false,
//             on_delete: None,
//             inline_link: None,
//         },
//         model_b: RelationFieldSpec {
//             name: "Post".to_string(),
//             field: None,
//             is_list: false,
//             is_optional: false,
//             on_delete: None,
//             inline_link: None,
//         },
//         table: None,
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

// #[test]
// fn full_CreateRelation_with_link_table_must_work() {
//     let json = r#"{
//         "stepType":"CreateRelation",
//         "name":"BlogToPosts",
//         "modelA": { "name":"Blog","field":"posts","isList":true,"onDelete":"SET_NULL","inlineLink":true},
//         "modelB": { "name":"Post","field":"blog","isOptional":true,"onDelete":"CASCADE"},
//         "table": { "modelAColumn":"blog", "modelBColumn":"post" }
//     }"#;
//     let expected_struct = MigrationStep::CreateRelation(CreateRelation {
//         name: "BlogToPosts".to_string(),
//         model_a: RelationFieldSpec {
//             name: "Blog".to_string(),
//             field: Some("posts".to_string()),
//             is_list: Some(true),
//             is_optional: false,
//             on_delete: Some("SET_NULL".to_string()),
//             inline_link: Some(true),
//         },
//         model_b: RelationFieldSpec {
//             name: "Post".to_string(),
//             field: Some("blog".to_string()),
//             is_list: false,
//             is_optional: Some(true),
//             on_delete: Some("CASCADE".to_string()),
//             inline_link: None,
//         },
//         table: Some(LinkTableSpec {
//             model_a_column: Some("blog".to_string()),
//             model_b_column: Some("post".to_string()),
//         }),
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

// #[test]
// fn CreateRelation_forcing_the_link_table_must_work() {
//     let json = r#"{
//         "stepType":"CreateRelation",
//         "name":"BlogToPosts",
//         "modelA": { "name":"Blog" },
//         "modelB": { "name":"Post" },
//         "table": { }
//     }"#;
//     let expected_struct = MigrationStep::CreateRelation(CreateRelation {
//         name: "BlogToPosts".to_string(),
//         model_a: RelationFieldSpec {
//             name: "Blog".to_string(),
//             field: None,
//             is_list: false,
//             is_optional: false,
//             on_delete: None,
//             inline_link: None,
//         },
//         model_b: RelationFieldSpec {
//             name: "Post".to_string(),
//             field: None,
//             is_list: false,
//             is_optional: false,
//             on_delete: None,
//             inline_link: None,
//         },
//         table: Some(LinkTableSpec {
//             model_a_column: None,
//             model_b_column: None,
//         }),
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

// #[test]
// fn DeletRelation_must_work() {
//     let json = r#"{"stepType":"DeleteRelation","name":"BlogToPost"}"#;
//     let expected_struct = MigrationStep::DeleteRelation(DeleteRelation {
//         name: "BlogToPost".to_string(),
//     });
//     assert_symmetric_serde(json, expected_struct);
// }

fn assert_symmetric_serde(json: &str, expected: MigrationStep) {
    let serde_value: serde_json::Value = serde_json::from_str(&json).expect("The provided input was invalid json.");
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
