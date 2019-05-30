mod common;
use common::*;
use datamodel::dml::*;

// Ported from
// https://github.com/prisma/prisma/blob/master/server/servers/deploy/src/test/scala/com/prisma/deploy/migration/validation/IdDirectiveSpec.scala

#[test]
fn id_without_strategy_should_use_defaults() {
    let dml = r#"
    model Model {
        id: ID @id
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("id")
        .assert_is_id(true)
        .assert_id_sequence(None)
        .assert_id_strategy(IdStrategy::Auto);
}

#[test]
fn id_with_explicit_auto_strategy() {
    let dml = r#"
    model Model {
        id: ID @id(strategy: AUTO)
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("id")
        .assert_is_id(true)
        .assert_id_sequence(None)
        .assert_id_strategy(IdStrategy::Auto);
}

#[test]
fn id_with_explicit_none_strategy() {
    let dml = r#"
    model Model {
        id: ID @id(strategy: NONE)
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("id")
        .assert_is_id(true)
        .assert_id_sequence(None)
        .assert_id_strategy(IdStrategy::None);
}

#[test]
fn id_should_also_work_on_embedded_types() {
    let dml = r#"
    model Model {
        id: ID @id

        @@embedded
    }
    "#;

    let datamodel = parse(dml);
    let user_model = datamodel.assert_has_model("Model");
    user_model
        .assert_has_field("id")
        .assert_is_id(true)
        .assert_id_sequence(None)
        .assert_id_strategy(IdStrategy::Auto);
}
