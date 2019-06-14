use crate::common::*;
use datamodel::{ast, errors::ValidationError};

#[test]
fn shound_fail_on_directive_duplication() {
    let dml = r#"
    type ID = String @id @default(cuid())

    model Model {
        id ID @id
    }
    "#;

    let error = parse_error(dml);

    error.assert_is_at(
        0,
        ValidationError::new_duplicate_directive_error("id", &ast::Span::new(23, 25)),
    );
    error.assert_is_at(
        1,
        ValidationError::new_duplicate_directive_error("id", &ast::Span::new(77, 79)),
    );
}

#[test]
fn shound_fail_on_directive_duplication_recursive() {
    let dml = r#"
    type MyStringWithDefault = String @default(cuid())
    type ID = MyStringWithDefault @id

    model Model {
        id ID @default(cuid())
    }
    "#;

    let error = parse_error(dml);

    error.assert_is_at(
        0,
        ValidationError::new_duplicate_directive_error("default", &ast::Span::new(40, 60)),
    );
    error.assert_is_at(
        1,
        ValidationError::new_duplicate_directive_error("default", &ast::Span::new(128, 148)),
    );
}

#[test]
fn shound_fail_on_endless_recursive_type_def() {
    let dml = r#"
    type MyString = ID
    type MyStringWithDefault = MyString
    type ID = MyStringWithDefault

    model Model {
        id ID 
    }
    "#;

    let error = parse_error(dml);

    error.assert_is(ValidationError::new_validation_error(
        "Recursive type definitions are not allowed. Recursive path was: ID -> MyStringWithDefault -> MyString -> ID",
        &ast::Span::new(21, 23),
    ));
}

#[test]
fn shound_fail_on_unresolvable_type() {
    let dml = r#"
    type MyString = Hugo
    type MyStringWithDefault = MyString
    type ID = MyStringWithDefault

    model Model {
        id ID 
    }
    "#;

    let error = parse_error(dml);

    error.assert_is(ValidationError::new_type_not_found_error(
        "Hugo",
        &ast::Span::new(21, 25),
    ));
}

#[test]
fn should_fail_on_custom_related_types() {
    let dml = r#"
    type UserViaEmail = User @relation(references: email)
    type UniqueString = String @unique

    model User {
        id Int @id
        email UniqueString
        posts Post[]
    }

    model Post {
        id Int @id
        user UserViaEmail
    }
    "#;

    let error = parse_error(dml);

    error.assert_is(ValidationError::new_validation_error(
        "Only scalar types can be used for defining custom types.",
        &ast::Span::new(25, 29),
    ));
}
