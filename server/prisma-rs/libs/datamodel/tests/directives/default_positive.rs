use crate::common::*;
use chrono::{DateTime, Utc};
use datamodel::common::{PrismaType, PrismaValue};

#[test]
fn should_set_default_for_all_scalar_types() {
    let dml = r#"
    model Model {
        id Int @id
        int Int @default(3)
        float Float @default(3.14)
        decimal Decimal @default(3.15)
        string String @default("String")
        boolean Boolean @default(false)
        dateTime DateTime @default("2019-06-17T14:20:57Z")
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("int")
        .assert_base_type(&PrismaType::Int)
        .assert_default_value(PrismaValue::Int(3));
    user_model
        .assert_has_field("float")
        .assert_base_type(&PrismaType::Float)
        .assert_default_value(PrismaValue::Float(3.14));
    user_model
        .assert_has_field("decimal")
        .assert_base_type(&PrismaType::Decimal)
        .assert_default_value(PrismaValue::Decimal(3.15));
    user_model
        .assert_has_field("string")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(PrismaValue::String(String::from("String")));
    user_model
        .assert_has_field("boolean")
        .assert_base_type(&PrismaType::Boolean)
        .assert_default_value(PrismaValue::Boolean(false));
    user_model
        .assert_has_field("dateTime")
        .assert_base_type(&PrismaType::DateTime)
        .assert_default_value(PrismaValue::DateTime(
            "2019-06-17T14:20:57Z".parse::<DateTime<Utc>>().unwrap(),
        ));
}

#[test]
fn should_set_default_an_enum_type() {
    let dml = r#"
    model Model {
        id Int @id
        role Role @default(ADMIN)
    }

    enum Role {
        ADMIN
        MODERATOR
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("role")
        .assert_enum_type("Role")
        .assert_default_value(PrismaValue::ConstantLiteral(String::from("ADMIN")));
}
