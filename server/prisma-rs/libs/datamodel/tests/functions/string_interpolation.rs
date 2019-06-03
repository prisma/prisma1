use crate::common::*;
use datamodel::dml;

#[test]
fn interpolate_expressions_in_strings() {
    let dml = r#"
    model User {
        id: ID @id
        firstName: String @default("user_${3}")
        lastName: String
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&dml::ScalarType::String)
        .assert_default_value(dml::Value::String(String::from("user_3")));
}

#[test]
fn dont_interpolate_escaped_expressions_in_strings() {
    let dml = r#"
    model User {
        id: ID @id
        firstName: String @default("user_\${3}")
        lastName: String
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&dml::ScalarType::String)
        .assert_default_value(dml::Value::String(String::from("user_${3}")));
}

#[test]
fn interpolate_functionals_in_strings() {
    let dml = r#"
    model User {
        id: ID @id
        firstName: String @default("user_${env("TEST_USER")}")
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
        .assert_default_value(dml::Value::String(String::from("user_prisma-user")));
}

#[test]
fn interpolate_nested_mess() {
    let dml = r#"
    model User {
        id: ID @id
        firstName: String @default("user_${ "number_${ "${ "really?_${3}" }" }" }")
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
        .assert_default_value(dml::Value::String(String::from("user_number_really?_3")));
}
