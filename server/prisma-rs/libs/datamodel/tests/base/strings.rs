use crate::common::*;
use datamodel::common::{PrismaType, PrismaValue};

#[test]
fn parse_basic_model() {
    let dml = r#"
    model User {
        id Int @id
        firstName String @default("This is a string with whitespace")
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_is_embedded(false);
    user_model
        .assert_has_field("firstName")
        .assert_base_type(&PrismaType::String)
        .assert_default_value(PrismaValue::String(String::from("This is a string with whitespace")));
}