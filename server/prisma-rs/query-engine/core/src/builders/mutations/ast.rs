//! Simple wrapper for WriteQueries

use crate::{
    builders::utils::{self, UuidString},
    BuilderExt, OneBuilder, ReadQuery,
};
use connector::mutaction::{
    DatabaseMutactionResult as MutationResult, Identifier, TopLevelDatabaseMutaction as RootMutation,
};
use graphql_parser::query::Field;
use prisma_models::{GraphqlId, ModelRef, PrismaArgs, PrismaValue};
use std::sync::Arc;

/// A structure to express mutation dependencies
///
/// When we run a mutation, it can have some prerequisites
/// to being runnable. The simplest examples of this is
/// when creating a record that has a required relation to
/// another model, which should also be created.
/// In this case, we first need to create the _other_ model,
/// then we can run the self mutation and turn the create
/// into a connect action, that is then merged into the
/// previous create.
///
/// This sounds complicated (and it is!) but we solve this in steps.
///
/// 1. We traverse the mutation AST and generate naive queries
/// 2. We let the [`look_ahead`] module pass over the tree which will
///    fold required connects into their update/ create parents,
///    as well as generating these `WriteQuerySet::Dependents` items.
/// 3. When the query pipeline yields mutations to the executor,
///    it then simply has to traverse this tree and execute dependent
///    mutations in reverse-order. This also needs to take id-mapping
///    into account (i.e. a create -> create needs to yield the child ID
///    to it's parent so it can finish the transaction)
///
/// To further understand how the mutation execution works, I recommend
/// reading the [`pipeline`] docs!
#[derive(Debug, Clone)]
pub enum WriteQuerySet {
    Query(WriteQuery),
    Dependents {
        self_: WriteQuery,
        next: Box<WriteQuerySet>,
    },
}

impl WriteQuerySet {
    /// Traverse through the `::Dependents` structure to inject
    /// a mutation at the last record (called base record)
    pub(crate) fn inject_at_base(&mut self, cb: impl FnOnce(&mut WriteQuery)) {
        match self {
            WriteQuerySet::Query(ref mut q) => {
                cb(q);
            }
            WriteQuerySet::Dependents { self_: _, next } => next.inject_at_base(cb),
        }
    }

    pub(crate) fn get_base_model(&self) -> ModelRef {
        match self {
            WriteQuerySet::Query(q) => match q.inner {
                RootMutation::CreateRecord(ref cn) => Arc::clone(&cn.model),
                _ => unimplemented!(),
            },
            WriteQuerySet::Dependents { self_: _, next } => next.get_base_model(),
        }
    }
}

/// A top-level write query (mutation)
#[derive(Debug, Clone)]
pub struct WriteQuery {
    /// The actual mutation object being built
    pub inner: RootMutation,

    /// The name of the WriteQuery
    pub name: String,

    /// Required to create following ReadQuery
    pub field: Field,
}

impl WriteQuery {
    pub fn model(&self) -> ModelRef {
        match self.inner {
            RootMutation::CreateRecord(ref record) => Arc::clone(&record.model),
            RootMutation::UpdateRecord(ref record) => record.where_.field.model.upgrade().unwrap(),
            RootMutation::DeleteRecord(ref record) => record.where_.field.model.upgrade().unwrap(),
            RootMutation::UpsertRecord(ref record) => record.where_.field.model.upgrade().unwrap(),
            RootMutation::UpdateManyRecords(ref records) => Arc::clone(&records.model),
            RootMutation::DeleteManyRecords(ref records) => Arc::clone(&records.model),
            _ => unimplemented!(),
        }
    }

    /// This function generates a pre-fetch `ReadQuery` for appropriate `WriteQuery` types
    pub fn generate_prefetch(&self) -> Option<ReadQuery> {
        match self.inner {
            RootMutation::DeleteRecord(_) => OneBuilder::new()
                .setup(self.model(), &self.field)
                .build()
                .ok()
                .map(|q| ReadQuery::RecordQuery(q)),
            _ => None,
        }
    }

    /// Generate a `ReadQuery` from the encapsulated `WriteQuery`
    pub fn generate_read(&self, res: MutationResult) -> Option<ReadQuery> {
        let field = match res.identifier {
            Identifier::Id(gql_id) => utils::derive_field(
                &self.field,
                self.model(),
                search_for_id(&self.inner).unwrap_or(gql_id),
                &self.name,
            ),
            Identifier::Count(_) => return None, // FIXME: We need to communicate count!
            Identifier::Record(_) => return None,
            _ => unimplemented!(),
        };

        match self.inner {
            // We ignore Deletes because they were already handled
            RootMutation::DeleteRecord(_) | RootMutation::DeleteManyRecords(_) => None,
            RootMutation::CreateRecord(_) | RootMutation::UpdateRecord(_) | RootMutation::UpsertRecord(_) => {
                OneBuilder::new()
                    .setup(self.model(), &field)
                    .build()
                    .ok()
                    .map(|q| ReadQuery::RecordQuery(q))
            }
            _ => unimplemented!(),
        }
    }
}

fn search_for_id(root: &RootMutation) -> Option<GraphqlId> {
    fn extract_id(model: &ModelRef, args: &PrismaArgs) -> Option<GraphqlId> {
        args.get_field_value(&model.fields().id().name).map(|pv| match pv {
            PrismaValue::GraphqlId(gqlid) => gqlid.clone(),
            PrismaValue::String(s) if s.is_uuid() => GraphqlId::UUID(s.clone().as_uuid()),
            PrismaValue::String(s) => GraphqlId::String(s.clone()),
            PrismaValue::Int(i) => GraphqlId::Int(*i as usize),
            _ => unreachable!(),
        })
    }

    match root {
        RootMutation::CreateRecord(cn) => extract_id(&cn.model, &cn.non_list_args),
        _ => None,
    }
}
