//! # ✨ Crunchtime hacks ✨
//!
//! This module evaluates a nested mutation and merges it's operations
//! into the current-level if it deems it neccessary.
//! This is to get around issues with required references
//! that are connected in a second step.

use crate::{CoreResult, MutationSet, WriteQuery};
use connector::mutaction::*;
use graphql_parser::query::Field;
use prisma_models::{ModelRef, PrismaArgs, PrismaValue, RelationFieldRef};

/// Either a nested operation or the startof a new dependency root
///
/// The problem is that when evaluating nested mutations,
/// we sometimes need to split one out into a new root operation
/// where reads/ writes are executed before it.
/// This is independent of the `flip_order` function and only
/// relevant for connect operations.
enum NestedReplace<T> {
    Nested(T),
    Root(MutationSet)
}

/// The QueryMonad encodes state of the `look_ahead` module
///
/// It performs operations on itself, meaning that they can yield both
/// errors as well as a change in state of the monad.
/// Functions are chained, but don't have to be called in any particular order.
pub struct QueryMonad(MutationSet);

impl QueryMonad {
    /// Create a new QueryMonad from a single WriteQuery
    pub fn from(input: WriteQuery) -> Self {
        Self(MutationSet::Query(input))
    }

    /// This function handles create -> connect fields
    pub fn create_connect(self) -> CoreResult<Self> {
        Ok(Self(match self.0 {
            MutationSet::Query(q) => {
                match q.inner {
                    TopLevelDatabaseMutaction::CreateNode(mut cn) => {
                        let connects = std::mem::replace(&mut cn.nested_mutactions.connects, vec![]);

                        for conn in connects {
                            let rf = cn.model
                                .fields()
                                .find_from_relation_fields(conn.relation_field.name.as_str())?;

                            // If the RelationField is the ID, then we can simply
                            // merge the operation into the parent because the
                            // foreign-key constraint will hold.
                            //
                            // However if it is not...then we need to yield a
                            // MutationSet::Dependents where there is a ReadQuery
                            // as a dependency to the actual WriteQuery.
                            // AND we need to preserve state to let the executor
                            // then figure out what information to add to the parent
                            if check_rf_is_id(&rf) {
                                let mut normal = vec![];
                                if rf.is_required {
                                    cn.non_list_args.insert(rf.name.clone(), conn.where_.value);
                                } else {
                                    normal.push(conn);
                                }
                                cn.nested_mutactions.connects = normal;

                                // Now recursively traverse all other creates
                                // for ncn in cn.nested_mutactions.creates.iter_mut() {
                                //     nested_create_connect(ncn)?;
                                // }

                                MutationSet::Query(WriteQuery {
                                    inner: TopLevelDatabaseMutaction::CreateNode(cn),
                                    ..q
                                })
                            } else {

                            }
                        }
                    }
                    _ => MutationSet::Query(q),
                }
            },
            who_even_cares => who_even_cares,
        }))
    }
}

fn

pub struct LookAhead;
impl LookAhead {
    pub fn eval(mut input: WriteQuery) -> CoreResult<MutationSet> {
        // input.inner = match input.inner {
        //     TopLevelDatabaseMutaction::CreateNode(mut cn) => {
        //         create_connect(&mut cn)?;
        //         TopLevelDatabaseMutaction::CreateNode(cn)
        //     }
        //     TopLevelDatabaseMutaction::UpdateNode(mut un) => {
        //         update_nested_connect(&mut un)?;
        //         TopLevelDatabaseMutaction::UpdateNode(un)
        //     }
        //     who_even_cares => who_even_cares,
        // };

        let flipped = flip_create_order(input);
        debug!("{:#?}", flipped);
        flipped
    }

    /// This function is called in the QueryExecutor, after executing a partial mutation tree
    ///
    /// What it needs to do is work with the result of the partial execution,
    /// then inject any IDs or data into the base mutation of the Dependents tree
    pub fn eval_partial(next: &mut MutationSet, self_: &WriteQuery, res: &DatabaseMutactionResult) -> CoreResult<()> {
        let name = next
            .get_base_model()
            .fields()
            .find_from_relation_fields(&self_.model().name.to_lowercase())?
            .name
            .clone();
        let id: PrismaValue = match res.identifier {
            Identifier::Id(ref gqlid) => gqlid.into(),
            _ => unimplemented!(),
        };

        next.inject_at_base(move |query| match query.inner {
            TopLevelDatabaseMutaction::CreateNode(ref mut cn) => cn.non_list_args.insert(name, id),
            _ => unimplemented!(),
        });

        Ok(())
    }
}

// /// Merge connect's on required fields into the create
// fn create_connect(cn: &mut CreateNode) -> CoreResult<()> {
//     let connects = std::mem::replace(&mut cn.nested_mutactions.connects, vec![]);

//     let mut new = vec![];
//     connect_fold_into(&cn.model, &mut cn.non_list_args, &mut new, connects.into_iter())?;
//     cn.nested_mutactions.connects = new;

//     // Now recursively traverse all other creates
//     for ncn in cn.nested_mutactions.creates.iter_mut() {
//         nested_create_connect(ncn)?;
//     }

//     Ok(())
// }

fn nested_create_connect(ncn: NestedCreateNode) -> CoreResult<Vec<NestedReplace<NestedCreateNode>>> {
    let connects = std::mem::replace(&mut ncn.nested_mutactions.connects, vec![]);
    for conn in connects {
        let rf = cn.model
            .fields()
            .find_from_relation_fields(conn.relation_field.name.as_str())?;

        if check_rf_is_id(&rf) {
            let mut normal = vec![];
            if rf.is_required {
                cn.non_list_args.insert(rf.name.clone(), conn.where_.value);
            } else {
                normal.push(conn);
            }
            cn.nested_mutactions.connects = normal;

            // Now recursively traverse all other creates
            for ncn in cn.nested_mutactions.creates.iter_mut() {
                nested_create_connect(ncn)?;
            }

            MutationSet::Query(WriteQuery {
                inner: TopLevelDatabaseMutaction::CreateNode(cn),
                ..q
            })
        } else {

        }
    }


    let connects = std::mem::replace(&mut ncn.nested_mutactions.connects, vec![]);
    let mut new = vec![];

    connect_fold_into(
        &ncn.relation_field.model(),
        &mut ncn.non_list_args,
        &mut new,
        connects.into_iter(),
    )?;

    ncn.nested_mutactions.connects = new;

    for cn in ncn.nested_mutactions.creates.iter_mut() {
        nested_create_connect(cn)?;
    }

    Ok(())
}

// fn update_nested_connect(un: &mut UpdateNode) -> CoreResult<()> {
//     for ncn in un.nested_mutactions.creates.iter_mut() {
//         nested_create_connect(ncn)?;
//     }

//     Ok(())
// }

fn flip_create_order(wq: WriteQuery) -> CoreResult<MutationSet> {
    match wq.inner {
        TopLevelDatabaseMutaction::CreateNode(mut cn) => {
            let creates = std::mem::replace(&mut cn.nested_mutactions.creates, vec![]);
            let (required, normal) = creates.into_iter().partition(|nc| {
                nc.relation_field.is_required
                    && check_should_flip(&cn.model, &nc.relation_field.related_field().model())
            });

            cn.nested_mutactions.creates = normal;

            let wq = WriteQuery {
                inner: TopLevelDatabaseMutaction::CreateNode(cn),
                ..wq
            };

            let wqs: MutationSet = required.into_iter().fold(MutationSet::Query(wq), |acc, req| match acc {
                MutationSet::Query(q) => MutationSet::Dependents {
                    self_: WriteQuery {
                        inner: hoist_nested_create(req),
                        name: q.name.clone(),
                        field: q.field.clone(),
                    },
                    next: Box::new(MutationSet::Query(q)),
                },
                MutationSet::Dependents { self_: _, ref next } => {
                    let (name, field) = get_name_field(&next);

                    MutationSet::Dependents {
                        self_: WriteQuery {
                            inner: hoist_nested_create(req),
                            name,
                            field,
                        },
                        next: Box::new(acc),
                    }
                }
                MutationSet::Read(_) => unimplemented!(),
            });

            Ok(wqs)
        }
        _ => Ok(MutationSet::Query(wq)),
    }
}

/// Takes a NestedCreateNode and turns it into a TopLevelDatabaseMutaction::CreateNode
fn hoist_nested_create(nc: NestedCreateNode) -> TopLevelDatabaseMutaction {
    TopLevelDatabaseMutaction::CreateNode(CreateNode {
        model: nc.relation_field.related_field().model(),
        non_list_args: nc.non_list_args,
        list_args: nc.list_args,
        nested_mutactions: nc.nested_mutactions,
    })
}

/// Small utility function to get a query name and field
fn get_name_field(next: &MutationSet) -> (String, Field) {
    match next {
        MutationSet::Dependents { self_: _, next } => get_name_field(&next),
        MutationSet::Query(q) => (q.name.clone(), q.field.clone()),
        MutationSet::Read(_) => unimplemented!(),
    }
}

fn check_should_flip(self_: &ModelRef, other: &ModelRef) -> bool {
    self_.name > other.name
}

fn check_rf_is_id(rf: &RelationFieldRef) -> bool {
    &rf.name == &rf.model().name
}
