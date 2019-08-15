use crate::common::*;
use datamodel::{ast::Span, errors::ValidationError};

// Ported from
// https://github.com/prisma/prisma/blob/master/server/servers/deploy/src/test/scala/com/prisma/deploy/migration/validation/IdDirectiveSpec.scala

#[test]
fn id_should_error_if_the_field_is_not_required() {
    let dml = r#"
    model Model {
        id Int? @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_directive_validation_error(
        "Fields that are marked as id must be required.",
        "id",
        Span::new(36, 38),
    ));
}

#[test]
fn id_should_error_if_an_unknown_strategy_is_used() {
    let dml = r#"
    model Model {
        id Int @id(strategy: FOO)
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_literal_parser_error(
        "id strategy",
        "FOO",
        Span::new(48, 51),
    ));
}

// DISABLED until we decide on this.
#[test]
fn id_should_error_on_model_without_id() {
    let dml = r#"
    model Model {
        id String
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Exactly one field must be marked as the id field with the `@id` directive.",
        "Model",
        Span::new(5, 42),
    ));
}

#[test]
fn id_should_error_multiple_ids_are_provided() {
    let dml = r#"
    model Model {
        id         Int      @id
        internalId String   @id @default(uuid())
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Exactly one field must be marked as the id field with the `@id` directive.",
        "Model",
        Span::new(5, 105),
    ));
}

const ID_TYPE_ERROR: &str =
    "Invalid ID field. ID field must be one of: Int @id, String @id @default(cuid()), String @id @default(uuid()).";

#[test]
fn id_should_error_if_the_id_field_is_not_of_valid_type() {
    let dml = r#"
    model Model {
        id DateTime @id
    }

    model Model2 {
        id Boolean @id
    }

    model Model3 {
        id Float @id
    }

    model Model4 {
        id Decimal @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is_at(
        0,
        ValidationError::new_model_validation_error(ID_TYPE_ERROR, "Model", Span::new(27, 42)),
    );

    errors.assert_is_at(
        1,
        ValidationError::new_model_validation_error(ID_TYPE_ERROR, "Model2", Span::new(77, 91)),
    );

    errors.assert_is_at(
        2,
        ValidationError::new_model_validation_error(ID_TYPE_ERROR, "Model3", Span::new(126, 138)),
    );

    errors.assert_is_at(
        3,
        ValidationError::new_model_validation_error(ID_TYPE_ERROR, "Model4", Span::new(173, 187)),
    );
}

#[test]
fn id_should_error_if_string_id_field_has_incorrect_default_value() {
    let dml = r#"
    model Model1 {
        id String @id
    }

    model Model2 {
        id String @id @default("hello")
    }

    model Model3 {
        id String @id @default("cuid")
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is_at(
        0,
        ValidationError::new_model_validation_error(ID_TYPE_ERROR, "Model1", Span::new(28, 41)),
    );

    errors.assert_is_at(
        1,
        ValidationError::new_model_validation_error(ID_TYPE_ERROR, "Model2", Span::new(76, 107)),
    );

    errors.assert_is_at(
        2,
        ValidationError::new_model_validation_error(ID_TYPE_ERROR, "Model3", Span::new(142, 172)),
    );
}
