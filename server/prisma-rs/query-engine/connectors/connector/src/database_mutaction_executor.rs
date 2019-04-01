use crate::{mutaction::*, ConnectorResult};
use prisma_models::{GraphqlId, SingleNode};
use serde_json::Value;

pub trait DatabaseMutactionExecutor {
    fn execute_nested(
        &self,
        db_name: String,
        mutaction: NestedDatabaseMutaction,
        parent_id: GraphqlId,
    ) -> ConnectorResult<DatabaseMutactionResults> {
        let mut results = DatabaseMutactionResults::default();

        match mutaction {
            NestedDatabaseMutaction::CreateNode(ref cn) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(self.execute_nested_create(db_name, &parent_id, cn)?),
                    typ: DatabaseMutactionResultType::Create,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                };

                results.push(result);
            }
            NestedDatabaseMutaction::UpdateNode(ref un) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(self.execute_nested_update(db_name, &parent_id, un)?),
                    typ: DatabaseMutactionResultType::Update,
                    mutaction: DatabaseMutaction::Nested(mutaction),
                };

                results.push(result);
            }
            NestedDatabaseMutaction::UpsertNode(_) => unimplemented!(),
            NestedDatabaseMutaction::DeleteNode(_) => unimplemented!(),
            NestedDatabaseMutaction::Connect(_) => unimplemented!(),
            NestedDatabaseMutaction::Disconnect(_) => unimplemented!(),
            NestedDatabaseMutaction::Set(_) => unimplemented!(),
            NestedDatabaseMutaction::UpdateNodes(_) => unimplemented!(),
            NestedDatabaseMutaction::DeleteNodes(_) => unimplemented!(),
        }

        Ok(results)
    }

    fn execute_toplevel(
        &self,
        db_name: String,
        mutaction: TopLevelDatabaseMutaction,
    ) -> ConnectorResult<DatabaseMutactionResults> {
        let mut results = DatabaseMutactionResults::default();

        match mutaction {
            TopLevelDatabaseMutaction::CreateNode(ref cn) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(self.execute_create(db_name, cn)?),
                    typ: DatabaseMutactionResultType::Create,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                };

                results.push(result);
            }
            TopLevelDatabaseMutaction::UpdateNode(ref un) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(self.execute_update(db_name, un)?),
                    typ: DatabaseMutactionResultType::Update,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                };

                results.push(result);
            }
            TopLevelDatabaseMutaction::UpsertNode(ref us) => {
                let (id, typ) = self.execute_upsert(db_name, us)?;

                let result = DatabaseMutactionResult {
                    identifier: Identifier::Id(id),
                    typ,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                };

                results.push(result);
            }
            TopLevelDatabaseMutaction::DeleteNode(ref dn) => {
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Node(self.execute_delete(db_name, dn)?),
                    typ: DatabaseMutactionResultType::Delete,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                };

                results.push(result);
            }
            TopLevelDatabaseMutaction::UpdateNodes(ref uns) => {
                // uns uns uns
                let result = DatabaseMutactionResult {
                    identifier: Identifier::Count(self.execute_update_many(db_name, uns)?),
                    typ: DatabaseMutactionResultType::Many,
                    mutaction: DatabaseMutaction::TopLevel(mutaction),
                };

                results.push(result);
            }
            TopLevelDatabaseMutaction::DeleteNodes(_) => unimplemented!(),
            TopLevelDatabaseMutaction::ResetData(_) => unimplemented!(),
        };

        Ok(results)
    }

    fn execute_raw(&self, query: String) -> ConnectorResult<Value>;

    fn execute_create(&self, db_name: String, mutaction: &CreateNode) -> ConnectorResult<GraphqlId>;
    fn execute_update(&self, db_name: String, mutaction: &UpdateNode) -> ConnectorResult<GraphqlId>;
    fn execute_delete(&self, db_name: String, mutaction: &DeleteNode) -> ConnectorResult<SingleNode>;
    fn execute_update_many(&self, db_name: String, mutaction: &UpdateNodes) -> ConnectorResult<usize>;

    fn execute_nested_create(
        &self,
        db_name: String,
        parent_id: &GraphqlId,
        mutaction: &NestedCreateNode,
    ) -> ConnectorResult<GraphqlId>;

    fn execute_nested_update(
        &self,
        db_name: String,
        parent_id: &GraphqlId,
        mutaction: &NestedUpdateNode,
    ) -> ConnectorResult<GraphqlId>;

    fn execute_upsert(
        &self,
        db_name: String,
        mutaction: &UpsertNode,
    ) -> ConnectorResult<(GraphqlId, DatabaseMutactionResultType)>;
}
