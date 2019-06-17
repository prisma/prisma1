use crate::common::*;
use datamodel::{ast::Span, errors::ValidationError};

#[test]
fn should_error_if_default_value_for_related() {
    let dml = r#"
    model Model {
        id Int @id
        rel A @default("")
    }

    model A {
        id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_directive_validation_error(
        "Cannot set a default value on a relation field.",
        "default",
        &Span::new(53, 64),
    ));
}

#[test]
fn should_error_if_default_value_for_list() {
    let dml = r#"
    model Model {
        id Int @id
        rel String[] @default(["hello"])
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_directive_validation_error(
        "Cannot set a default value on list field.",
        "default",
        &Span::new(60, 78),
    ));
}

#[test]
fn should_error_if_default_value_type_missmatch() {
    let dml = r#"
    model Model {
        id Int @id
        rel String @default(3)
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_directive_validation_error(
        "Expected a String value, but received numeric value \"3\".",
        "default",
        &Span::new(66, 67),
    ));
}

#[test]
fn should_error_if_default_value_parser_error() {
    let dml = r#"
    model Model {
        id Int @id
        rel DateTime @default("Hugo")
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_directive_validation_error(
        "Expected a datetime value, but failed while parsing \"Hugo\": input contains invalid characters.",
        "default",
        &Span::new(68, 74),
    ));
}
