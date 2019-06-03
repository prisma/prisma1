mod common;
use common::*;

#[test]
fn should_treat_single_values_as_arrays_of_length_oneca() {
    let dml = r#"
    model User {
        id: ID @id
        posts: Post[]
    }

    model Post {
        id: ID @id
        user: User @relation(references: id)
    }
    "#;

    let schema = parse(dml);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["id"]);
}