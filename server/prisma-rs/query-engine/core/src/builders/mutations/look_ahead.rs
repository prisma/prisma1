//! # ✨ Crunchtime hacks ✨
//!
//! This module evaluates a nested mutation and merges it's operations
//! into the current-level if it deems it neccessary.
//! This is to get around issues with required references
//! that are connected in a second step.

#![allow(warnings)]

use crate::{
    builders::{utils, NestedValue, ValueList, ValueMap, ValueSplit},
    CoreError, CoreResult, ManyNestedBuilder, SimpleNestedBuilder, UpsertNestedBuilder, WriteQuery,
};
use connector::{filter::NodeSelector, mutaction::* /* ALL OF IT */};
use graphql_parser::query::{Field, Value};
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, PrismaArgs, PrismaValue};

use std::{collections::BTreeMap, sync::Arc};

pub struct LookAhead;
impl LookAhead {
    pub fn eval(input: TopLevelDatabaseMutaction) -> CoreResult<TopLevelDatabaseMutaction> {
        Ok(match input {
            TopLevelDatabaseMutaction::CreateNode(mut cn) => {
                create_connect(&mut cn)?;
                TopLevelDatabaseMutaction::CreateNode(cn)
            }
            TopLevelDatabaseMutaction::UpdateNode(mut un) => {
                update_nested_connect(&mut un)?;
                TopLevelDatabaseMutaction::UpdateNode(un)
            }
            who_even_cares => who_even_cares,
        })
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
