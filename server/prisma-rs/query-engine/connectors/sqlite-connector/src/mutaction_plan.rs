use connector::*;
use parking_lot::RwLock;
use prisma_models::prelude::GraphqlId;
use prisma_query::ast::{Column, Query, Table};
use std::sync::Arc;

type ReturnSwitch = Arc<RwLock<Returning>>;

#[derive(Debug, Clone)]
pub enum Returning {
    Expected,
    Got(GraphqlId),
}

impl Returning {
    fn new() -> ReturnSwitch {
        Arc::new(RwLock::new(Returning::Expected))
    }

    pub fn set(&mut self, id: Option<GraphqlId>) {
        if let Some(id) = id {
            *self = Returning::Got(id);
        }
    }
}

#[derive(Debug, Clone)]
pub struct MutactionStep {
    pub query: Query,
    pub table: Table,
    pub returning: Option<(Column, ReturnSwitch)>,
    pub needing: Option<(Column, ReturnSwitch)>,
}

#[derive(Debug, Clone)]
pub struct MutactionPlan {
    pub steps: Vec<MutactionStep>,
    pub mutaction: DatabaseMutaction,
}

impl From<DatabaseMutaction> for MutactionPlan {
    fn from(mutaction: DatabaseMutaction) -> MutactionPlan {
        match mutaction {
            DatabaseMutaction::TopLevel(top_level_mutaction) => MutactionPlan::from(top_level_mutaction),
            _ => panic!("Only top level mutactions are supported"),
        }
    }
}

impl From<TopLevelDatabaseMutaction> for MutactionPlan {
    fn from(mutaction: TopLevelDatabaseMutaction) -> MutactionPlan {
        match mutaction {
            TopLevelDatabaseMutaction::CreateNode(create_node) => MutactionPlan::from(create_node),
        }
    }
}

impl From<CreateNode> for MutactionPlan {
    fn from(mutaction: CreateNode) -> MutactionPlan {
        let insert = MutationBuilder::create_node(mutaction.model.clone(), mutaction.non_list_args.clone());
        let id_column = mutaction.model.fields().id().as_column();

        let mut steps = vec![MutactionStep {
            query: Query::from(insert),
            table: mutaction.model.table(),
            returning: Some((id_column, Returning::new())),
            needing: None,
        }];

        for (field_name, list_value) in mutaction.list_args.clone() {
            let field = mutaction.model.fields().find_from_scalar(&field_name).unwrap();
            let table = field.scalar_list_table();
            let insert = MutationBuilder::create_scalar_list_value(table.clone(), list_value);

            let needing = steps
                .get(0)
                .and_then(|step| step.returning.as_ref().map(|r| Arc::clone(&r.1)))
                .map(|returning| (table.node_id_column(), returning));

            steps.push(MutactionStep {
                query: Query::from(insert),
                table: table.table(),
                returning: None,
                needing: needing,
            })
        }

        MutactionPlan {
            steps: steps,
            mutaction: DatabaseMutaction::from(mutaction),
        }
    }
}
