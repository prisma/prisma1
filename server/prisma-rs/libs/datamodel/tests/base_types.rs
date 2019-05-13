mod common;
use common::*;
use datamodel::dml;

#[test]
fn parse_scalar_types() {
    let dml = r#"
    model User {
        firstName: String
        age: Int
        isPro: Boolean
        balance: Decimal
        averageGrade: Float
    }
    "#;

    let schema = parse_and_validate(dml);
    let user_model = schema.assert_has_model("User");
    user_model.assert_has_field("firstName").assert_base_type(&dml::ScalarType::String);
    user_model.assert_has_field("age").assert_base_type(&dml::ScalarType::Int);
    user_model.assert_has_field("isPro").assert_base_type(&dml::ScalarType::Boolean);
    user_model.assert_has_field("balance").assert_base_type(&dml::ScalarType::Decimal);
    user_model.assert_has_field("averageGrade").assert_base_type(&dml::ScalarType::Float);
}

#[test]
fn parse_field_arity() {
    let dml = r#"
    model Post {
        text: String
        photo: String?
        comments: String[]
    }
    "#;

    let schema = parse_and_validate(dml);
    let post_model = schema.assert_has_model("Post");
    post_model.assert_has_field("text").assert_base_type(&dml::ScalarType::String).assert_arity(&dml::FieldArity::Required);
    post_model.assert_has_field("photo").assert_base_type(&dml::ScalarType::String).assert_arity(&dml::FieldArity::Optional);
    post_model.assert_has_field("comments").assert_base_type(&dml::ScalarType::String).assert_arity(&dml::FieldArity::List);
}