mod common;
use common::*;

#[test]
fn should_add_back_relations() {
    let dml = r#"
    model User {
        id: ID @id
        posts: Post[]
    }

    model Post {
        post_id: ID @id
    }
    "#;

    let schema = parse(dml);
    let post_model = schema.assert_has_model("Post");
    post_model.assert_has_field("user").assert_relation_to("User");
    // No normalization of to_fields for now.
    //.assert_relation_to_fields(&["id"]);
}

#[test]
#[ignore] // No normalization of to_fields for now.
fn should_add_to_fields_on_the_correct_side_tie_breaker() {
    let dml = r#"
    model User {
        id: ID @id
        post: Post
    }

    model Post {
        post_id: ID @id
        user: User
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("post")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&[]);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["id"]);
}

#[test]
#[ignore] // No normalization of to_fields for now.
fn should_add_to_fields_on_the_correct_side_list() {
    let dml = r#"
    model User {
        id: ID @id
        post: Post
    }

    model Post {
        post_id: ID @id
        user: User[]
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("post")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&["post_id"]);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&[]);
}

#[test]
fn should_camel_case_back_relation_field_name() {
    let dml = r#"
    model OhWhatAUser {
        id: ID @id
        posts: Post[]
    }

    model Post {
        post_id: ID @id
    }
    "#;

    let schema = parse(dml);
    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("ohWhatAUser")
        .assert_relation_to("OhWhatAUser");
}
