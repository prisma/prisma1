use crate::migration::datamodel_migration_steps_inferrer::DataModelMigrationStepsInferrer;
use prisma_datamodel::dml::*;

    #[test]
    fn bla() {
        let dm1 = "";
        let dm2 = "";
        let steps = DataModelMigrationStepsInferrer::infer(previous: &Schema, next: &Schema);
        // TODO: assert something on the steps
    }


    // TODO: we will need this in a lot of test files. Extract it.
    fn parse(datamodelString: String) -> Schema {
        let ast = prisma_datamodel::parser::parse(&datamodelString);
        // TODO: this would need capabilities
        let validator = Validator::new();
        validator.validate(&ast);
    }