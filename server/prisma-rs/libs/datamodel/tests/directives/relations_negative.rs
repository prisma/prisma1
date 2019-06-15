use crate::common::*;
use datamodel::{ast::Span, errors::ValidationError};

#[test]
fn should_fail_on_ambiguous_relations() {
    let dml = r#"
    model User {
        id Int @id
        posts Post[]
        more_posts Post[]
    }

    model Post {
        post_id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous relation detected.",
        "User",
        &Span::new(45, 57),
    ));
}

#[test]
fn should_fail_on_ambiguous_named_relations() {
    let dml = r#"
    model User {
        id Int @id
        posts Post[] @relation(name: "test")
        more_posts Post[] @relation(name: "test")
    }

    model Post {
        post_id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous relation detected.",
        "User",
        &Span::new(45, 81),
    ));
}

#[test]
fn should_fail_on_ambiguous_named_relations_2() {
    let dml = r#"
    model User {
        id Int @id
        posts Post[] @relation(name: "a")
        more_posts Post[] @relation(name: "b")
        some_posts Post[]
        even_more_posts Post[] @relation(name: "a")
    }

    model Post {
        post_id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous relation detected.",
        "User",
        &Span::new(45, 78),
    ));
}

#[test]
fn should_fail_on_ambiguous_self_relation() {
    let dml = r#"
    model User {
        id Int @id
        father User
        son User
        mother User
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous self relation detected.",
        "User",
        &Span::new(45, 56),
    ));
}

#[test]
fn should_fail_on_ambiguous_named_self_relation() {
    let dml = r#"
    model User {
        id Int @id
        father User @relation(name: "family")
        son User @relation(name: "family")
        mother User @relation(name: "family")
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous self relation detected.",
        "User",
        &Span::new(45, 82),
    ));
}

#[test]
fn should_fail_on_conflicting_back_relation_field_name() {
    let dml = r#"
    model User {
        id Int @id
        posts Post[] @relation(name: "test")
        more_posts Post[]
    }

    model Post {
        post_id Int @id
        user User @relation(name: "test")
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Automatic opposite related field generation would cause a naming conflict. Please add an explicit opposite relation field.",
        "User",
        &Span::new(90, 107),
    ));
}

#[test]
#[ignore]
// This case is caught by the requirement that named relations
// need to have an opposite field.
fn should_fail_on_conflicting_generated_back_relation_fields() {
    // More specifically, this should not panic.
    let dml = r#"
    model Todo {
        id Int @id
        author Owner @relation(name: "AuthorTodo")
        delegatedTo Owner? @relation(name: "DelegatedToTodo")
    }

    model Owner {
        id Int @id
        todos Todo[]
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is_at(0, ValidationError::new_model_validation_error(
        "Automatic opposite related field generation would cause a naming conflict. Please add an explicit opposite relation field.",
        "Todo",
        &Span::new(98, 152),
    ));
}

#[test]
fn should_fail_on_named_generated_back_relation_fields() {
    // More specifically, this should not panic.
    let dml = r#"
    model Todo {
        id Int @id
        author Owner @relation(name: "AuthorTodo")
    }

    model Owner {
        id Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is_at(
        0,
        ValidationError::new_model_validation_error(
            "Named relations require an opposite field.",
            "Todo",
            &Span::new(45, 87),
        ),
    );
}
