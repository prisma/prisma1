use connector::*;
use parking_lot::RwLock;
use prisma_models::prelude::GraphqlId;
use prisma_query::ast::{Column, Query};
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
    pub returning: Option<ReturnSwitch>,
    pub needing: Option<(Column, ReturnSwitch)>,
}

impl MutactionStep {
    fn returning_ref(&self) -> Option<ReturnSwitch> {
        self.returning.as_ref().map(Arc::clone)
    }
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

        let mut steps = vec![MutactionStep {
            query: Query::from(insert),
            returning: Some(Returning::new()),
            needing: None,
        }];

        for (field_name, list_value) in mutaction.list_args.clone() {
            let field = mutaction.model.fields().find_from_scalar(&field_name).unwrap();
            let table = field.scalar_list_table();
            let insert = MutationBuilder::create_scalar_list_value(table.clone(), list_value);

            let needing = steps
                .get(0)
                .and_then(|step| step.returning_ref())
                .map(|returning| (table.node_id_column(), returning));

            steps.push(MutactionStep {
                query: Query::from(insert),
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
