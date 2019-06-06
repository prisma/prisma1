use crate::common::*;
use datamodel::common::{PrismaType, PrismaValue};

#[test]
fn should_apply_a_custom_type() {
    let dml = r#"
    type ID = String @id @default(cuid())

    model Model {
        id: ID
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("id")
        .assert_is_id(true)
        .assert_base_type(&PrismaType::String)
        .assert_id_sequence(None)
        .assert_default_value(PrismaValue::Expression(
            String::from("cuid"),
            PrismaType::String,
            Vec::new(),
        ));
}

#[test]
fn should_recursively_apply_a_custom_type() {
    let dml = r#"
    type MyString = String
    type MyStringWithDefault = MyString @default(cuid())
    type ID = MyStringWithDefault @id

    model Model {
        id: ID
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("id")
        .assert_is_id(true)
        .assert_base_type(&PrismaType::String)
        .assert_id_sequence(None)
        .assert_default_value(PrismaValue::Expression(
            String::from("cuid"),
            PrismaType::String,
            Vec::new(),
        ));
}

#[test]
fn should_be_able_to_handle_multiple_types() {
    let dml = r#"
    type ID = String @id @default(cuid())
    type UniqueString = String @unique
    type Cash = Int @default(0)

    model User {
        id       ID
        email    UniqueString
        balance  Cash
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("User");
    user_model
        .assert_has_field("id")
        .assert_is_id(true)
        .assert_base_type(&PrismaType::String)
        .assert_id_sequence(None)
        .assert_default_value(PrismaValue::Expression(
            String::from("cuid"),
            PrismaType::String,
            Vec::new(),
        ));

    user_model
        .assert_has_field("email")
        .assert_is_unique(true)
        .assert_base_type(&PrismaType::String);

    user_model
        .assert_has_field("balance")
        .assert_base_type(&PrismaType::Int)
        .assert_default_value(PrismaValue::Int(0));
}

#[test]
fn should_be_able_to_define_custom_related_types() {
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

    let datamodel = parse(dml);

    let user_model = datamodel.assert_has_model("User");

    user_model
        .assert_has_field("email")
        .assert_is_unique(true)
        .assert_base_type(&PrismaType::String);

    user_model
        .assert_has_field("posts")
        .assert_relation_to("Post")
        .assert_arity(&datamodel::dml::FieldArity::List);

    let post_model = datamodel.assert_has_model("Post");

    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["email"]);
}

#[test]
fn should_be_able_to_define_custom_enum_types() {
    let dml = r#"
    type RoleWithDefault = Role @default(USER)

    model User {
        id Int @id
        role RoleWithDefault
    }

    enum Role {
        ADMIN
        USER
        CEO
    }
    "#;

    let datamodel = parse(dml);

    let user_model = datamodel.assert_has_model("User");

    user_model
        .assert_has_field("role")
        .assert_enum_type("Role")
        .assert_default_value(PrismaValue::ConstantLiteral(String::from("USER")));
}
