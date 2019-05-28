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
            "Id fields can not be optional.", 
            "id", 
            &Span::new(35, 43)
        )
    );
}