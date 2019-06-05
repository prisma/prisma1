use crate::common::*;
use datamodel::{ast::Span, errors::ValidationError};

#[test]
fn should_fail_on_ambiguous_relations() {
    let dml = r#"
    model User {
        id: Int @id
        posts: Post[]
        more_posts: Post[]
    }

    model Post {
        post_id: Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous relation detected.",
        "User",
        &Span::new(46, 68),
    ));
}

#[test]
fn should_fail_on_ambiguous_named_relations() {
    let dml = r#"
    model User {
        id: Int @id
        posts: Post[] @relation(name: "test")
        more_posts: Post[] @relation(name: "test")
    }

    model Post {
        post_id: Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous relation detected.",
        "User",
        &Span::new(46, 83),
    ));
}

#[test]
fn should_fail_on_ambiguous_named_relations_2() {
    let dml = r#"
    model User {
        id: Int @id
        posts: Post[] @relation(name: "a")
        more_posts: Post[] @relation(name: "b")
        some_posts: Post[]
        even_more_posts: Post[] @relation(name: "a")
    }

    model Post {
        post_id: Int @id
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous relation detected.",
        "User",
        &Span::new(46, 80),
    ));
}

#[test]
fn should_fail_on_ambiguous_self_relation() {
    let dml = r#"
    model User {
        id: Int @id
        father: User
        son: User
        mother: User
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous self relation detected.",
        "User",
        &Span::new(46, 67),
    ));
}

#[test]
fn should_fail_on_ambiguous_named_self_relation() {
    let dml = r#"
    model User {
        id: Int @id
        father: User @relation(name: "family")
        son: User @relation(name: "family")
        mother: User @relation(name: "family")
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Ambiguous self relation detected.",
        "User",
        &Span::new(46, 84),
    ));
}

#[test]
fn should_fail_on_conflicting_back_relation_field_name() {
    let dml = r#"
    model User {
        id: Int @id
        posts: Post[] @relation(name: "test")
        more_posts: Post[]
    }

    model Post {
        post_id: Int @id
        user: User@relation(name: "test")
    }
    "#;

    let errors = parse_error(dml);

    errors.assert_is(ValidationError::new_model_validation_error(
        "Automatic back field generation would cause a naming conflict.",
        "Post",
        &Span::new(168, 201),
    ));
}
