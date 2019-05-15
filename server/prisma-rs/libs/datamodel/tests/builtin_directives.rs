mod common;
use common::*;

#[test]
fn db_directive() {
    let dml = r#"
    model User {
        firstName: String @db("first_name")
    }
    @db("user")

    model Post {
        text: String @db(name: "post_text")
    }
    @db(name: "posti")
    "#;

    let schema = parse_and_validate(dml);
    let user_model = schema.assert_has_model("User").assert_with_db_name("user");
    user_model
        .assert_has_field("firstName")
        .assert_with_db_name("first_name");

    let post_model = schema.assert_has_model("Post").assert_with_db_name("posti");
    post_model.assert_has_field("text").assert_with_db_name("post_text");
}
