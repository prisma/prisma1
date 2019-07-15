use crate::common::*;
use datamodel::{common::PrismaType, dml};

#[test]
fn allow_multiple_relations() {
    let dml = r#"
    model User {
        id Int @id
        more_posts Post[] @relation(name: "more_posts")
        posts Post[]
    }

    model Post {
        id Int @id
        text String
        user User
        posting_user User @relation(name: "more_posts")
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("posts")
        .assert_relation_to("Post")
        .assert_arity(&dml::FieldArity::List)
        .assert_relation_name("PostToUser");
    user_model
        .assert_has_field("more_posts")
        .assert_relation_to("Post")
        .assert_arity(&dml::FieldArity::List)
        .assert_relation_name("more_posts");

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("text")
        .assert_base_type(&PrismaType::String);
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_arity(&dml::FieldArity::Required)
        .assert_relation_name("PostToUser");
    post_model
        .assert_has_field("posting_user")
        .assert_relation_to("User")
        .assert_arity(&dml::FieldArity::Required)
        .assert_relation_name("more_posts");
}

#[test]
fn allow_complicated_self_relations() {
    let dml = r#"
    model User {
        id Int @id
        son User @relation(name: "offspring")
        father User @relation(name: "offspring")
        husband User @relation(name: "spouse")
        wife User @relation(name: "spouse")
    }
    "#;

    let schema = parse(dml);

    let user_model = schema.assert_has_model("User");
    user_model.assert_has_field("son").assert_relation_to("User");
    user_model.assert_has_field("father").assert_relation_to("User");
    user_model.assert_has_field("husband").assert_relation_to("User");
    user_model.assert_has_field("wife").assert_relation_to("User");
}
