#![allow(non_snake_case)]
mod test_harness;
use test_harness::*;

#[test]
fn unapply_must_work() {
    test_each_connector(|engine| {
        let dm1 = r#"
            model Test {
                id String @id
                field String
            }
        "#;
        let result1 = infer_and_apply(&engine, &dm1);
        assert_eq!(result1.table_bang("Test").column("field").is_some(), true);

        let dm2 = r#"
            model Test {
                id String @id
            }
        "#;
        let result2 = infer_and_apply(&engine, &dm2);
        assert_eq!(result2.table_bang("Test").column("field").is_some(), false);

        let result3 = unapply_migration(&engine);
        assert_eq!(result1, result3);

        // reapply the migration again
        let result4 = infer_and_apply(&engine, &dm2);
        assert_eq!(result2, result4);
    });
}
