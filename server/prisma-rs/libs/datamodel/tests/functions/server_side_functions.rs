use crate::common::*;
use datamodel::common::{PrismaValue, PrismaType};

#[test]
fn correctly_handle_server_side_now_function() {
    let dml = r#"
    model User {
        id: Int @id
        signupDate: DateTime @default(now())
    }
    "#;

    std::env::set_var("TEST_USER", "prisma-user");

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("signupDate")
        .assert_base_type(&PrismaType::DateTime)
        .assert_default_value(PrismaValue::Expression(
            String::from("now"),
            PrismaType::DateTime,
            vec![]
        ));
}

#[test]
fn correctly_handle_server_side_cuid_function() {
    let dml = r#"
    model User {
        id: Int @id
        someId: String @default(cuid())
    }
    "#;

    std::env::set_var("TEST_USER", "prisma-user");

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("someId")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(PrismaValue::Expression(
            String::from("cuid"),
            PrismaType::String,
            vec![]
        ));
}
