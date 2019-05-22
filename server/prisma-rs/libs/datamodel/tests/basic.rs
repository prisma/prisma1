mod common;
use common::*;
use datamodel::dml;

#[test]
fn parse_basic_model() {
    let dml = r#"
    model User {
        firstName: String
        lastName: String
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&dml::ScalarType::String);
    user_model
        .assert_has_field("lastName")
        .assert_base_type(&dml::ScalarType::String);
}

#[test]
fn parse_basic_enum() {
    let dml = r#"
    enum Roles {
        ADMIN
        USER
    }
    "#;

    let schema = parse(dml);
    let role_enum = schema.assert_has_enum("Roles");
    role_enum.assert_has_value("ADMIN");
    role_enum.assert_has_value("USER");
}
