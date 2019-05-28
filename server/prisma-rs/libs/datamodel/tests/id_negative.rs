mod common;
use common::*;
use datamodel::{dml::*, errors::ValidationError, ast::Span};

// Ported from 
// https://github.com/prisma/prisma/blob/master/server/servers/deploy/src/test/scala/com/prisma/deploy/migration/validation/IdDirectiveSpec.scala

#[test]
fn id_should_error_if_the_field_is_not_required() {
    let dml = r#"
    model Model {
        id: ID? @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(
        ValidationError::new_directive_validation_error(
            "Fields that are marked as id must be required.", 
            "id", 
            &Span::new(35, 43)
        )
    );
}

#[test]
fn id_should_error_if_an_unknown_strategy_is_used() {
    let dml = r#"
    model Model {
        id: ID @id(strategy: FOO)
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(
        ValidationError::new_literal_parser_error(
            "id strategy", 
            "FOO", 
            &Span::new(48, 51)
        )
    );
}

#[test]
fn id_should_error_on_model_without_id() {
    let dml = r#"
    model Model {
        id: ID
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(
        ValidationError::new_model_validation_error(
            "One field must be marked as the id field with the `@id` directive.", 
            "Model",
            &Span::new(5, 44)
        )
    );
}
