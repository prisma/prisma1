mod common;
use common::*;
use datamodel::dml;

#[test]
fn allow_multiple_relations() {
    let dml = r#"
    model User {
        id: ID @id
        more_posts: Post[] @relation(name: "more_posts")
        posts: Post[]
    }

    model Post {
        id: ID @id
        text: String
        user: User
        posting_user: User @relation(name: "more_posts")
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("posts")
        .assert_relation_to("Post")
        .assert_arity(&dml::FieldArity::List);
    user_model
        .assert_has_field("more_posts")
        .assert_relation_to("Post")
        .assert_arity(&dml::FieldArity::List);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("text")
        .assert_base_type(&dml::ScalarType::String);
    post_model.assert_has_field("user").assert_relation_to("User");
}

#[test]
fn allow_complicated_self_relations() {
    let dml = r#"
    model User {
        id: ID @id
        son: User @relation(name: "offspring")
        father: User @relation(name: "offspring")
        husband: User @relation(name: "spouse")
        wife: User @relation(name: "spouse")
    }
    "#;

    let schema = parse(dml);

    let user_model = schema.assert_has_model("User");
    user_model.assert_has_field("son").assert_relation_to("User");
    user_model.assert_has_field("father").assert_relation_to("User");
    user_model.assert_has_field("husband").assert_relation_to("User");
    user_model.assert_has_field("wife").assert_relation_to("User");
}
