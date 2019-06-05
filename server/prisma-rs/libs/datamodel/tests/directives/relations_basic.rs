use crate::common::*;
use datamodel::{common::PrismaType, dml};

#[test]
fn resolve_relation() {
    let dml = r#"
    model User {
        id: Int @id
        firstName: String
        posts: Post[]
    }

    model Post {
        id: Int @id
        text: String
        user: User
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String);
    user_model
        .assert_has_field("posts")
        .assert_relation_to("Post")
        .assert_arity(&dml::FieldArity::List);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("text")
        .assert_base_type(&PrismaType::String);
    post_model.assert_has_field("user").assert_relation_to("User");
}

#[test]
fn resolve_related_field() {
    let dml = r#"
    model User {
        id: Int @id
        firstName: String
        posts: Post[]
    }

    model Post {
        id: Int @id
        text: String
        user: User @relation(references: [firstName])
    }
    "#;

    let schema = parse(dml);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["firstName"]);
}

#[test]
fn resolve_related_fields() {
    let dml = r#"
    model User {
        id: Int @id
        firstName: String
        lastName: String
        posts: Post[]
    }

    model Post {
        id: Int @id
        text: String
        user: User @relation(references: [firstName, lastName])
    }
    "#;

    let schema = parse(dml);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["firstName", "lastName"]);
}

#[test]
fn resolve_enum_field() {
    let dml = r#"
    model User {
        id: Int @id
        email: String
        role: Role
    }

    enum Role {
        ADMIN
        USER
        PRO
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("email")
        .assert_base_type(&PrismaType::String);
    user_model.assert_has_field("role").assert_enum_type("Role");

    let role_enum = schema.assert_has_enum("Role");
    role_enum.assert_has_value("ADMIN");
    role_enum.assert_has_value("PRO");
    role_enum.assert_has_value("USER");
}
