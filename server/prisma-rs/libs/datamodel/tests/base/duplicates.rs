use crate::common::*;
use datamodel::{ast::Span, errors::ValidationError};

#[test]
fn fail_on_duplicate_models() {
    let dml = r#"
    model User {
        id Int @id
    }
    model User {
        id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_duplicate_top_error(
        "model",
        "model",
        "User",
        &Span::new(53, 57),
    ));
}
#[test]
fn fail_on_model_enum_conflict() {
    let dml = r#"
    enum User {
        Admin
        Moderator
    }
    model User {
        id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_duplicate_top_error(
        "model",
        "enum",
        "User",
        &Span::new(65, 69),
    ));
}
#[test]
fn fail_on_model_type_conflict() {
    let dml = r#"
    type User = String
    model User {
        id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_duplicate_top_error(
        "model",
        "type",
        "User",
        &Span::new(34, 38),
    ));
}

#[test]
fn fail_on_duplicate_field() {
    let dml = r#"
    model User {
        id Int @id
        firstName String
        firstName String
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_duplicate_field_error(
        "User",
        "firstName",
        &Span::new(70, 79),
    ));
}

#[test]
fn fail_on_duplicate_enum_value() {
    let dml = r#"
    enum Role {
        Admin
        Moderator
        Moderator
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_duplicate_enum_value_error(
        "Role",
        "Moderator",
        &Span::new(57, 66),
    ));
}
