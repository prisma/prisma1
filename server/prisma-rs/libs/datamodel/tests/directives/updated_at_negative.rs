use crate::common::*;
use datamodel::{ast::Span, errors::ValidationError};

#[test]
fn should_fail_if_field_type_is_string() {
    let dml = r#"
    model User {
        id Int @id
        lastSeen String @updatedAt
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_directive_validation_error(
        "Fields that are marked with @updatedAt must be of type DateTime.",
        "updatedAt",
        &Span::new(62, 71),
    ));
}

#[test]
fn should_fail_if_field_arity_is_list() {
    let dml = r#"
    model User {
        id Int @id
        lastSeen DateTime[] @updatedAt
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_directive_validation_error(
        "Fields that are marked with @updatedAt can not be lists.",
        "updatedAt",
        &Span::new(66, 75),
    ));
}
