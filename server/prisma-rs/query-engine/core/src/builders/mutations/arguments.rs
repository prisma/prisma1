//! Provides scoped arguments for mutations

#![allow(warnings)]

use crate::{CoreError, CoreResult};
use graphql_parser::query::Value;
use prisma_models::{PrismaListValue, PrismaValue};
use std::collections::BTreeMap;

/// Scoped arguments are either leafs or branches

#[derive(Debug, Clone)]
pub enum ScopedArg<'name> {
    Node(ScopedArgNode<'name>),
    Value(PrismaValue),
}

#[derive(Debug, Clone)]
pub struct ListArg<'name>(String, Option<Vec<ScopedArg<'name>>>);

impl<'name> From<&'name ListArg<'name>> for (String, PrismaListValue) {
    fn from(vla: &'name ListArg<'name>) -> Self {
        (
            vla.0.clone(),
            match vla.1 {
                Some(ref vec) => Some(
                    vec.iter()
                        .filter_map(|sa| match sa {
                            ScopedArg::Value(val) => Some(val.clone()),
                            _ => None,
                        })
                        .collect(),
                ),
                None => None,
            },
        )
    }
}

pub fn convert_tree<'name>(inval: &BTreeMap<String, ScopedArg<'name>>) -> BTreeMap<String, PrismaValue> {
    inval
        .iter()
        .filter_map(|(key, val)| {
            (match val {
                ScopedArg::Value(val) => Some((key.clone(), val.clone())),
                _ => None,
            })
        })
        .collect()
}

/// A node that holds some mutation arguments
#[derive(Debug, Default, Clone)]
pub struct ScopedArgNode<'name> {
    // Scope debug information
    pub name: &'name str,

    // Top-level attributes
    pub data: BTreeMap<String, ScopedArg<'name>>,
    pub lists: Vec<ListArg<'name>>,

    // Nested attributes
    pub create: BTreeMap<String, ScopedArg<'name>>,
    pub update: BTreeMap<String, ScopedArg<'name>>,
    pub upsert: BTreeMap<String, ScopedArg<'name>>,
    pub delete: BTreeMap<String, ScopedArg<'name>>,
    pub connect: BTreeMap<String, ScopedArg<'name>>,
    pub disconnect: BTreeMap<String, ScopedArg<'name>>,
}

impl<'name> ScopedArgNode<'name> {
    /// Take a non-root node and pivot it's data to pretend to be a root node
    ///
    /// What this means is that all Scalar-fields (single values and lists) are
    /// associated into the `data` and `lists` fields, while all nested operations
    /// remain the same.
    ///
    /// This way it's possible to call `convert_tree` on a nested data set
    /// without having to do manual sorting or cleaning
    pub fn pivot_root(&self) -> Self {
        let mut clone = self.clone();

        // Split an iterator, then filter out `Option` values and push to `data` vector
        macro_rules! split {
            ($target:expr, $data:ident) => {
                let (swap, tmp): (Vec<_>, Vec<_>) = std::mem::replace(&mut $target, Default::default())
                    .into_iter()
                    .map(|(k, v)| match v {
                        ScopedArg::Node(n) => (Some((k, ScopedArg::Node(n))), None),
                        ScopedArg::Value(v) => (None, Some((k, v))),
                    })
                    .unzip();
                $target = swap.into_iter().filter_map(|a| a).collect();
                tmp.into_iter().for_each(|d| {
                    $data.push(d);
                });
            };
        }

        let mut root = vec![];

        split!(clone.create, root);
        split!(clone.update, root);
        split!(clone.upsert, root);
        split!(clone.delete, root);
        split!(clone.connect, root);
        split!(clone.disconnect, root);

        // Split the scalars into `data` and `lists`
        let (data, _lists): (Vec<_>, Vec<_>) = root
            .into_iter()
            .filter_map(|a| a)
            .map(|(key, val)| match val {
                PrismaValue::List(l) => (None, Some((key, ScopedArg::Value(PrismaValue::List(l))))),
                value => (Some((key, ScopedArg::Value(value))), None),
            })
            .unzip();

        // Then filter out the marker-Options and store data
        clone.data = data.into_iter().filter_map(|a| a).collect();

        // FIXME: self.lists isn't stored yet because in the nested parsing code
        //        we never actually invoke `handle_scalar_list` which is kinda essential
        //        to being able to make this re-association.
        //        For now it'll only work with normal Scalars

        clone
    }

    /// Split a BTree of node-value mixes into seperate trees for values and nodes
    pub fn filter_nested(
        args: &BTreeMap<String, ScopedArg<'name>>,
    ) -> (BTreeMap<String, ScopedArgNode<'name>>, BTreeMap<String, PrismaValue>) {
        let (nested, vals): (Vec<_>, Vec<_>) = args
            .into_iter()
            .map(|(key, val)| match val {
                ScopedArg::Node(node) => (Some((key.clone(), node.clone())), None),
                ScopedArg::Value(val) => (None, Some((key.clone(), val.clone()))),
            })
            .unzip();

        (
            nested.into_iter().filter_map(|a| a).collect(),
            vals.into_iter().filter_map(|a| a).collect(),
        )
    }
}

impl<'name> ScopedArg<'name> {
    /// Parse a set of GraphQl input arguments
    pub fn parse(args: &'name Vec<(String, Value)>) -> CoreResult<ScopedArgNode> {
        match Self::evaluate_root(args.iter().filter(|(arg, _)| arg.as_str() != "where").collect()) {
            ScopedArg::Node(node) => Ok(node),
            ScopedArg::Value(v) => Err(CoreError::QueryValidationError(format!(
                "Invalid root attribute `{:?}`!",
                v
            ))),
        }
    }

    /// The root of an argument tree is part of a top-level-mutation
    ///
    /// It can only contain the keys `data` and `where` but because
    /// `where` clauses are handled elsewhere,
    /// here we only care about `data`
    fn evaluate_root(args: Vec<&'name (String, Value)>) -> Self {
        args.iter()
            .fold(ScopedArg::Node(Default::default()), |_, (name, value)| {
                match (name.as_str(), value) {
                    ("data", Value::Object(obj)) => {
                        ScopedArg::Node(obj.iter().fold(Default::default(), |mut node, (key, value)| {
                            node.name = name.as_str();
                            match value {
                                // Handle scalar-list arguments
                                Value::Object(obj) if obj.contains_key("set") => {
                                    node.lists.push(handle_scalar_list(&key, obj));
                                }
                                // Handle nested arguments
                                Value::Object(obj) => {
                                    node.data.insert(key.clone(), Self::evaluate_tree(key.as_str(), obj));
                                }
                                // Single data scalars
                                value => {
                                    node.data
                                        .insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                                }
                            }
                            node
                        }))
                    }
                    (key, _) => panic!("Unexpected attribute key `{}`", key),
                }
            })
    }

    /// Determine whether a subtree needs to be expanded into it's own node
    fn evaluate_tree(name: &'name str, obj: &'name BTreeMap<String, Value>) -> Self {
        ScopedArg::Node(obj.iter().fold(Default::default(), |mut node, (key, val)| {
            node.name = name;
            match (key.as_str(), val) {
                ("create", Value::Object(obj)) => obj.iter().for_each(|(key, val)| match val {
                    Value::Object(ref obj) => {
                        node.create.insert(key.clone(), Self::evaluate_tree(key, obj));
                    }
                    value => {
                        node.create
                            .insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                    }
                }),
                ("update", Value::Object(obj)) => obj.iter().for_each(|(key, val)| match val {
                    Value::Object(ref obj) => {
                        node.update.insert(key.clone(), Self::evaluate_tree(key, obj));
                    }
                    value => {
                        node.update
                            .insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                    }
                }),
                ("upsert", Value::Object(obj)) => obj.iter().for_each(|(key, val)| match val {
                    Value::Object(ref obj) => {
                        node.upsert.insert(key.clone(), Self::evaluate_tree(key, obj));
                    }
                    value => {
                        node.upsert
                            .insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                    }
                }),
                ("delete", Value::Object(obj)) => obj.iter().for_each(|(key, val)| match val {
                    Value::Object(ref obj) => {
                        node.delete.insert(key.clone(), Self::evaluate_tree(key, obj));
                    }
                    value => {
                        node.delete
                            .insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                    }
                }),
                ("connect", Value::Object(obj)) => obj.iter().for_each(|(key, val)| match val {
                    Value::Object(ref obj) => {
                        node.connect.insert(key.clone(), Self::evaluate_tree(key, obj));
                    }
                    value => {
                        node.connect
                            .insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                    }
                }),
                ("disconnect", Value::Object(obj)) => obj.iter().for_each(|(key, val)| match val {
                    Value::Object(ref obj) => {
                        node.disconnect.insert(key.clone(), Self::evaluate_tree(key, obj));
                    }
                    value => {
                        node.disconnect
                            .insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                    }
                }),
                (verb, _) => panic!("Unknown verb `{}`", verb),
            }

            node
        }))
    }
}

/// Parse a `{ "set": [...] }` structure into a ScalarListSet
fn handle_scalar_list<'name>(name: &String, obj: &'name BTreeMap<String, Value>) -> ListArg<'name> {
    ListArg(
        name.clone(),
        match obj.get("set") {
            Some(Value::List(l)) => Some(
                l.iter()
                    .map(|v| PrismaValue::from_value(v))
                    .map(|pv| ScopedArg::Value(pv))
                    .collect::<Vec<_>>(),
            ),
            None => None,
            _ => None, // TODO: This should maybe return an error
        },
    )
}
