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
use prisma_models::{Field as ModelField, InternalDataModelRef, ModelRef, PrismaValue, Project};

use std::{collections::BTreeMap, sync::Arc};

pub struct LookAhead;
impl LookAhead {
    pub fn eval(input: TopLevelDatabaseMutaction) -> CoreResult<TopLevelDatabaseMutaction> {
        Ok(match input {
            TopLevelDatabaseMutaction::CreateNode(mut cn) => {
                create_connect(&mut cn)?;
                TopLevelDatabaseMutaction::CreateNode(cn)
            }
            who_even_cares => who_even_cares,
        })
    }
}

/// Merge connect's on required fields into the create
fn create_connect(cn: &mut CreateNode) -> CoreResult<()> {
    let connects = std::mem::replace(&mut cn.nested_mutactions.connects, vec![]);

    let mut new = vec![];
    for conn in connects.into_iter() {
        let rf = cn
            .model
            .fields()
            .find_from_relation_fields(conn.relation_field.name.as_str())?;
        if rf.is_required {
            cn.non_list_args.insert(rf.name.clone(), conn.where_.value);
        } else {
            new.push(conn);
        }
    }

    cn.nested_mutactions.connects = new;
    Ok(())
}
