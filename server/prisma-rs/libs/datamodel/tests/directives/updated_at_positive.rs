use crate::common::*;
use datamodel::common::PrismaType;

#[test]
fn should_apply_updated_at_directive() {
    let dml = r#"
    model User {
        id: Int @id
        lastSeen DateTime @updatedAt
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("lastSeen")
        .assert_base_type(&PrismaType::DateTime)
        .assert_is_updated_at(true);
    user_model.assert_has_field("id").assert_is_updated_at(false);
}
