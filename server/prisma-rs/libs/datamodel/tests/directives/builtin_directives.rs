use crate::common::*;
use datamodel::common::PrismaType;

#[test]
fn db_directive() {
    let dml = r#"
    model User {
        id: Int @id
        firstName: String @db("first_name")

        @@db("user")
    }

    model Post {
        id: Int @id
        text: String @db(name: "post_text")

        @@db(name: "posti")
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User").assert_with_db_name("user");
    user_model
        .assert_has_field("firstName")
        .assert_with_db_name("first_name");

    let post_model = schema.assert_has_model("Post").assert_with_db_name("posti");
    post_model.assert_has_field("text").assert_with_db_name("post_text");
}

#[test]
fn unique_directive() {
    let dml = r#"
        model Test {
            id: String @id
            unique: String @unique
        }
    "#;

    let schema = parse(dml);
    let test_model = schema.assert_has_model("Test");

    test_model
        .assert_has_field("id")
        .assert_base_type(&PrismaType::String)
        .assert_is_unique(false)
        .assert_is_id(true);
    test_model
        .assert_has_field("unique")
        .assert_base_type(&PrismaType::String)
        .assert_is_unique(true);
}
