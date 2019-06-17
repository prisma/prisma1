//! # ✨ Crunchtime hacks ✨
//!
//! This module evaluates a nested mutation and merges it's operations
//! into the current-level if it deems it neccessary.
//! This is to get around issues with required references
//! that are connected in a second step.

#![allow(warnings)]

use crate::{
    builders::{utils, NestedValue, ValueList, ValueMap, ValueSplit},
    CoreError, CoreResult, ManyNestedBuilder, SimpleNestedBuilder, UpsertNestedBuilder, WriteQuery, WriteQuerySet
};
use connector::{filter::NodeSelector, mutaction::* /* ALL OF IT */};
use graphql_parser::query::{Field, Value};
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, PrismaArgs, PrismaValue};

use std::{collections::BTreeMap, sync::Arc};

pub struct LookAhead;
impl LookAhead {
    pub fn eval(mut input: WriteQuery) -> CoreResult<WriteQuerySet> {
        input.inner = match input.inner {
            TopLevelDatabaseMutaction::CreateNode(mut cn) => {
                create_connect(&mut cn)?;
                TopLevelDatabaseMutaction::CreateNode(cn)
            }
            TopLevelDatabaseMutaction::UpdateNode(mut un) => {
                update_nested_connect(&mut un)?;
                TopLevelDatabaseMutaction::UpdateNode(un)
            }
            who_even_cares => who_even_cares,
        };

        flip_create_order(input)
    }
}

/// Merge connect's on required fields into the create
fn create_connect(cn: &mut CreateNode) -> CoreResult<()> {
    let connects = std::mem::replace(&mut cn.nested_mutactions.connects, vec![]);

    let mut new = vec![];
    connect_fold_into(&cn.model, &mut cn.non_list_args, &mut new, connects.into_iter())?;
    cn.nested_mutactions.connects = new;

    // Now recursively traverse all other creates
    for ncn in cn.nested_mutactions.creates.iter_mut() {
        nested_create_connect(ncn)?;
    }

    Ok(())
}

fn nested_create_connect(ncn: &mut NestedCreateNode) -> CoreResult<()> {
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

fn update_nested_connect(un: &mut UpdateNode) -> CoreResult<()> {
    for ncn in un.nested_mutactions.creates.iter_mut() {
        nested_create_connect(ncn)?;
    }

    Ok(())
}

fn flip_create_order(ncn: NestedCreateNode) -> CoreResult<()> {
    Ok(())
}

fn flip_create_order(wq: WriteQuery) -> CoreResult<WriteQuerySet> {
    match wq.inner {
        TopLevelDatabaseMutaction::CreateNode(mut cn) => {
            let creates = std::mem::replace(&mut cn.nested_mutactions.creates, vec![]);
            let mut normal = vec![];
            let mut required = vec![];
            for nc in creates.into_iter() {
                if nc.relation_field.is_required {
                    required.push(nc);
                } else {
                    normal.push(nc);
                }
            }
            cn.nested_mutactions.creates = normal;

            let wq = WriteQuery {
                inner: TopLevelDatabaseMutaction::CreateNode(cn),
                ..wq
            };

            let wqs: WriteQuerySet = required
                .into_iter()
                .fold(WriteQuerySet::Query(wq), |mut acc, req| {
                    match acc {
                        WriteQuerySet::Query(q) => WriteQuerySet::Dependents {
                            self_: WriteQuery {
                                inner: hoist_nested_create(req),
                                name: q.name.clone(),
                                field: q.field.clone(),
                            },
                            next: Box::new(WriteQuerySet::Query(q))
                        },
                        WriteQuerySet::Dependents { self_: _, ref next } => {
                            let (name, field) = get_name_field(&next);

                            WriteQuerySet::Dependents {
                                self_: WriteQuery {
                                    inner: hoist_nested_create(req),
                                    name,
                                    field
                                },
                                next: Box::new(acc)
                            }
                        }
                    }
                });

            Ok(wqs)
        },
        _ => unimplemented!()
    }
}

fn hoist_nested_create(nc: NestedCreateNode) -> TopLevelDatabaseMutaction {
    TopLevelDatabaseMutaction::CreateNode(CreateNode {
        model: nc.relation_field.model(),
        non_list_args: nc.non_list_args,
        list_args: nc.list_args,
        nested_mutactions: nc.nested_mutactions,
    })
}

fn get_name_field(next: &WriteQuerySet) -> (String, Field) {
    match next {
        WriteQuerySet::Dependents { self_: _, next } => get_name_field(&next),
        WriteQuerySet::Query(q) => (q.name.clone(), q.field.clone())
    }
}

/// Fold require `connect` operations into their parent
///
/// This function requires the model definition of the parent,
/// an argument set to fold into, a vector for non-required operations
/// and a set of connects to evaluate.
fn connect_fold_into(
    model: &ModelRef,
    args: &mut PrismaArgs,
    nested: &mut Vec<NestedConnect>,
    connects: impl Iterator<Item = NestedConnect>,
) -> CoreResult<()> {
    for conn in connects {
        let rf = model
            .fields()
            .find_from_relation_fields(conn.relation_field.name.as_str())?;

        if rf.is_required {
            args.insert(rf.name.clone(), conn.where_.value);
        } else {
            nested.push(conn);
        }
    }

    Ok(())
}
