use crate::{mutaction::*, ConnectorResult};
use prisma_models::GraphqlId;
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute(&self, db_name: String, mutaction: DatabaseMutaction) -> ConnectorResult<DatabaseMutactionResults> {
        let mut results = DatabaseMutactionResults::default();

        match mutaction {
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::CreateNode(ref cn)) => {
                let result = DatabaseMutactionResult {
                    id_or_count: IdOrCount::Id(self.execute_create(db_name, cn)?),
                    typ: DatabaseMutactionResultType::Create,
                    mutaction: mutaction,
                };

                results.push(result);
            }
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpdateNode(ref un)) => {
                let result = DatabaseMutactionResult {
                    id_or_count: IdOrCount::Id(self.execute_update(db_name, un)?),
                    typ: DatabaseMutactionResultType::Update,
                    mutaction: mutaction,
                };

                results.push(result)
            }
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpsertNode(_)) => unimplemented!(),
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::DeleteNode(_)) => unimplemented!(),
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpdateNodes(_)) => unimplemented!(),
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::DeleteNodes(_)) => unimplemented!(),
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::ResetData(_)) => unimplemented!(),
            DatabaseMutaction::Nested(_) => panic!("nested mutactions are not supported yet!"),
        };

        Ok(results)
    }

    fn execute_raw(&self, query: String) -> ConnectorResult<Value>;
    fn execute_create(&self, db_name: String, mutaction: &CreateNode) -> ConnectorResult<GraphqlId>;
    fn execute_update(&self, db_name: String, mutaction: &UpdateNode) -> ConnectorResult<GraphqlId>;
}
