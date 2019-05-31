mod common;
use common::*;
use datamodel::dml;

#[test]
fn interpolate_environment_variables() {
    let dml = r#"
    model User {
        id: ID @id
        firstName: String @default(env("TEST_USER"))
        lastName: String
    }
    "#;

    std::env::set_var("TEST_USER", "prisma-user");

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&dml::ScalarType::String)
        .assert_default_value(dml::Value::String(String::from("prisma-user")));
}

// This is very useless, except being a good test case.
#[test]
fn interpolate_nested_environment_variables() {
    let dml = r#"
    model User {
        id: ID @id
        firstName: String @default(env(env("TEST_USER_VAR")))
        lastName: String
    }
    "#;

    std::env::set_var("TEST_USER_VAR", "TEST_USER");
    std::env::set_var("TEST_USER", "prisma-user");

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&dml::ScalarType::String)
        .assert_default_value(dml::Value::String(String::from("prisma-user")));
}
