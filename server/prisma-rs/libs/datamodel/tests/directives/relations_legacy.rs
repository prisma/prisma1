use crate::common::*;
use datamodel::dml;
use datamodel::{ast::Span, errors::ValidationError};

// Ported from
// https://github.com/prisma/prisma/blob/master/server/servers/deploy/src/test/scala/com/prisma/deploy/migration/validation/RelationDirectiveSpec.scala

// TODO: Split up to existing relation files.

#[test]
fn succeed_without_directive_if_unambigous() {
    let dml = r#"
    model Todo {
      id: ID @id
      title: String
      comments: Comment[]
    }
    
    model Comment {
      id: ID @id
      text: String
    }
    "#;

    let schema = parse(dml);
    let todo_model = schema.assert_has_model("Todo");
    todo_model
        .assert_has_field("comments")
        .assert_relation_to("Comment")
        .assert_arity(&dml::FieldArity::List);

    let comment_model = schema.assert_has_model("Comment");
    comment_model
    .assert_has_field("todo")
    .assert_arity(&dml::FieldArity::Optional)
    .assert_relation_to("Todo");
}

#[test]
fn fail_if_back_relation_for_embedded_type() {
    let dml = r#"
    model Todo {
      id: ID @id
      title: String
      comments: Comment[]
    }
    
    model Comment {
      id: ID @id
      text: String
      todo: Todo
      
      @@embedded
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Embedded models cannot have back relation fields.",
        "Comment",
        &Span::new(154, 178),
    ));
}

#[test]
fn settings_must_be_deteced() {
    let dml = r#"
    model Todo {
      id: ID @id
      todo: Todo? @relation(name: "MyRelation", onDelete: CASCADE)
    }
    "#;

    let schema = parse(dml);

    let todo_model = schema.assert_has_model("Todo");
    todo_model
        .assert_has_field("todo")
        .assert_relation_to("Todo")
        .assert_arity(&dml::FieldArity::Optional)
        .assert_relation_delete_strategy(dml::OnDeleteStrategy::Cascade);
}

#[test]
fn fail_if_ambigous_relation_fields_do_not_sepecify_a_name() {
    let dml = r#"
    model Todo {
      id: ID @id
      comments: Comment[]
      comments2: Comment[]
    }
    
    model Comment {
      id: ID @id
      text: String
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Embedded models cannot have back relation fields.",
        "Comment",
        &Span::new(154, 178),
    ));
}
