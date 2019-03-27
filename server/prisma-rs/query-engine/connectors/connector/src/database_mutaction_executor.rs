use crate::{mutaction::*, ConnectorResult};
use prisma_models::{GraphqlId, SingleNode};
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute(&self, db_name: String, mutaction: DatabaseMutaction) -> ConnectorResult<DatabaseMutactionResults> {
        let mut results = DatabaseMutactionResults::default();

        match mutaction {
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::CreateNode(ref cn)) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(self.execute_create(db_name, cn)?),
                    typ: DatabaseMutactionResultType::Create,
                    mutaction,
                };

                results.push(result);
            }
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpdateNode(ref un)) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(self.execute_update(db_name, un)?),
                    typ: DatabaseMutactionResultType::Update,
                    mutaction,
                };

                results.push(result);
            }
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpsertNode(ref us)) => {
                let (id, typ) = self.execute_upsert(db_name, us)?;

                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(id),
                    typ,
                    mutaction,
                };

                results.push(result);
            }
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::DeleteNode(ref dn)) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Node(self.execute_delete(db_name, dn)?),
                    typ: DatabaseMutactionResultType::Delete,
                    mutaction,
                };

                results.push(result);
            }
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::UpdateNodes(ref uns)) => {
                // uns uns uns
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Count(self.execute_update_many(db_name, uns)?),
                    typ: DatabaseMutactionResultType::Many,
                    mutaction,
                };

                results.push(result);
            }
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::DeleteNodes(_)) => unimplemented!(),
            DatabaseMutaction::TopLevel(TopLevelDatabaseMutaction::ResetData(_)) => unimplemented!(),
            DatabaseMutaction::Nested(_) => panic!("nested mutactions are not supported yet!"),
        };

        Ok(results)
    }

    fn execute_raw(&self, query: String) -> ConnectorResult<Value>;

    fn execute_create(&self, db_name: String, mutaction: &CreateNode) -> ConnectorResult<GraphqlId>;
    fn execute_update(&self, db_name: String, mutaction: &UpdateNode) -> ConnectorResult<GraphqlId>;
    fn execute_delete(&self, db_name: String, mutaction: &DeleteNode) -> ConnectorResult<SingleNode>;

    fn execute_update_many(&self, db_name: String, mutaction: &UpdateNodes) -> ConnectorResult<usize>;

    fn execute_upsert(
        &self,
        db_name: String,
        mutaction: &UpsertNode,
    ) -> ConnectorResult<(GraphqlId, DatabaseMutactionResultType)>;
}
