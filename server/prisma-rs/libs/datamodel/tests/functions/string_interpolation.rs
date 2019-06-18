use crate::common::*;
use datamodel::{
    ast::Span,
    common::{PrismaType, PrismaValue},
    dml,
    errors::ValidationError,
};

#[test]
fn interpolate_expressions_in_strings() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("user_${3}")
        lastName String
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(dml::Value::String(String::from("user_3")));
}

#[test]
fn dont_interpolate_escaped_expressions_in_strings() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("user_\${3}")
        lastName String
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(dml::Value::String(String::from("user_${3}")));
}

#[test]
fn interpolate_functionals_in_strings() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("user_${env("TEST_USER")}")
        lastName String
    }
    "#;

    std::env::set_var("TEST_USER", "prisma-user");

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(dml::Value::String(String::from("user_prisma-user")));
}

#[test]
fn interpolate_nested_mess() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("user_${ "number_${ "${ "really?_${3}" }" }" }")
        lastName String
    }
    "#;

    std::env::set_var("TEST_USER", "prisma-user");

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(dml::Value::String(String::from("user_number_really?_3")));
}

#[test]
fn should_not_remove_whitespace() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("This is a string with whitespace")
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(PrismaValue::String(String::from("This is a string with whitespace")));
}

#[test]
fn should_not_try_to_interpret_comments_in_strings() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("This is a string with a // Comment")
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(PrismaValue::String(String::from("This is a string with a // Comment")));
}

#[test]
fn resolve_argument_errors_correctly() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("user_${env("UNKNOWN_FOR_SURE")}")
        lastName String
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is_at(
        0,
        ValidationError::new_functional_evaluation_error(
            "Environment variable not found: \"UNKNOWN_FOR_SURE\".",
            &Span::new(83, 101),
        ),
    );
}

#[test]
fn resolve_array_interpolation_errors_correctly() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("user_${["Hello"]}")
        lastName String
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is_at(
        0,
        ValidationError::new_validation_error("Arrays cannot be interpolated into strings.", &Span::new(79, 88)),
    );
}
