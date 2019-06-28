//! A mutation parser module
//!
//! **Note** for whomever it may concearn: this parser module would potentially
//! be interesting to expand for regular read-queries as well.
//! It parses the graphql specific AST into our own data representation.
//! While this is a slim conversion, it does make working with it easier
//! and provide a single place to do error handling and validaiton.
//!
//! The module exposes a few wrapper types that map as follows
//!
//! | Module type       | Mapped type                     |
//! |-------------------|---------------------------------|
//! | ValueMap          | `BTreeMap<String, Value>`       |
//! | ValueList         | `(String, Option<Vec<Value>>)`  |
//! | ValueSplit        | Splitting `values`, `nested`, and `lists` into a `struct` |
//!
//! Handled mutation arguments are then encoded in the `NestedValue`
//! return type which specifies the type of mutation (query) being handled.
//!
//! A `ValueSplit` represents all data from a "level",
//! but split into it's respective components to be easier to work with.
//!
//! - `values: Normal scalar values
//! - `lists`: Scalarlist values that are passed seperately
//! - `nested`: All nested child records

use connector::filter::RecordFinder;
use graphql_parser::query::Value;
use prisma_models::{ModelRef, PrismaValue};
use std::collections::BTreeMap;

/// A set of values
#[derive(Debug, Clone)]
pub struct ValueMap(pub BTreeMap<String, Value>);

#[derive(Debug, Clone)]
pub struct ValueList(String, Option<Vec<Value>>);

#[derive(Debug, Clone)]
pub struct ValueSplit {
    /// All values that map to simple scalars
    pub values: ValueMap,
    /// All values that are nested objects
    pub nested: ValueMap,
    /// All values that are scalar lists
    pub lists: Vec<ValueList>,
}

impl ValueList {
    ///
    pub fn convert(self) -> (String, Option<Vec<PrismaValue>>) {
        (
            self.0,
            match self.1 {
                Some(vec) => Some(vec.into_iter().map(|v| PrismaValue::from_value_broken(&v)).collect()),
                None => None,
            },
        )
    }
}

impl From<&Vec<(String, Value)>> for ValueMap {
    fn from(vec: &Vec<(String, Value)>) -> Self {
        Self(vec.into_iter().map(|k| k.clone()).collect())
    }
}

impl From<&Value> for ValueMap {
    fn from(val: &Value) -> Self {
        Self(match val {
            Value::Object(obj) => obj.into_iter().map(|(k, v)| (k.clone(), v.clone())).collect(),
            _ => panic!("Unsupported `ValueMap` initiaisation!"),
        })
    }
}

impl ValueMap {
    pub fn split(self) -> ValueSplit {
        let (nested, vals): (Vec<_>, Vec<_>) = self
            .0
            .iter()
            .map(|(key, val)| match val {
                Value::Object(obj) if obj.contains_key("set") => (None, Some((key, val))),
                Value::Object(_) => (Some((key, val)), None),
                value => (None, Some((key, value))),
            })
            .unzip();

        let (values, lists): (Vec<_>, Vec<_>) = vals
            .into_iter()
            .filter_map(|a| a)
            .map(|(key, val)| match val {
                Value::Object(ref obj) if obj.contains_key("set") => (
                    None,
                    Some(ValueList(
                        key.clone(),
                        match obj.get("set") {
                            Some(Value::List(l)) => Some(l.clone()),
                            None => None,
                            _ => None, // TODO: This should maybe return an error
                        },
                    )),
                ),
                value => (Some((key, value)), None),
            })
            .unzip();

        ValueSplit {
            nested: ValueMap(
                nested
                    .into_iter()
                    .filter_map(|a| a)
                    .map(|(a, b)| (a.clone(), b.clone()))
                    .collect(),
            ),
            values: ValueMap(
                values
                    .into_iter()
                    .filter_map(|a| a)
                    .map(|(a, b)| (a.clone(), b.clone()))
                    .collect(),
            ),
            lists: lists.into_iter().filter_map(|a| a).collect(),
        }
    }

    pub fn to_prisma_values(self) -> BTreeMap<String, PrismaValue> {
        self.0
            .into_iter()
            .map(|(k, v)| (k, PrismaValue::from_value_broken(&v)))
            .collect()
    }

    pub fn to_record_finder(&self, model: ModelRef) -> Option<RecordFinder> {
        self.0
            .iter()
            .filter_map(|(field, value)| {
                model
                    .fields()
                    .find_from_scalar(&field)
                    .ok()
                    .map(|f| (f, PrismaValue::from_value_broken(&value)))
            })
            .nth(0)
            .map(|(field, value)| RecordFinder { field, value })
    }
}

//////////////////////////////////////////////////

#[derive(Debug, Clone)]
pub enum NestedValue {
    Simple {
        name: String,
        kind: String,
        map: ValueMap,
    },
    Block {
        name: String,
        kind: String,
        data: ValueMap,
        where_: ValueMap,
    },
    Many {
        name: String,
        kind: String,
        list: Vec<ValueMap>,
    },
    Upsert {
        name: String,
        create: ValueMap,
        update: ValueMap,
        where_: ValueMap,
    },
}

impl ValueMap {
    /// Extract mutation arguments from a value map
    pub fn eval_tree(&self, self_name: &str) -> Vec<NestedValue> {
        let mut vec = Vec::new();

        // Go through all the objects on this level
        for (name, value) in self.0.iter() {
            println!("Evaluting name: {}", name);

            // We KNOW that we are only dealing with objects
            let obj = match value {
                Value::Object(obj) => obj,
                _ => unreachable!(),
            };

            // These are actions (create, update, ...)
            for (action, nested) in obj.iter() {
                // We handle upserts specifically because they're weird
                if action == "upsert" {
                    let name = name.clone();
                    let (create, update, where_) = match nested {
                        Value::Object(obj) => match (obj.get("create"), obj.get("update"), obj.get("where")) {
                            (Some(Value::Object(create)), Some(Value::Object(update)), Some(Value::Object(where_))) => {
                                (
                                    ValueMap(create.clone()),
                                    ValueMap(update.clone()),
                                    ValueMap(where_.clone()),
                                )
                            }
                            _ => unreachable!(),
                        },
                        _ => unreachable!(),
                    };

                    vec.push(NestedValue::Upsert {
                        name,
                        create,
                        update,
                        where_,
                    });
                } else {
                    match nested {
                        Value::Object(obj) => {
                            vec.push(NestedValue::Simple {
                                name: name.clone(),
                                kind: action.clone(),
                                map: ValueMap(obj.clone()),
                            });
                        }
                        Value::List(list) => {
                            let mut buf = vec![];

                            for obj in list {
                                let obj = match obj {
                                    Value::Object(o) => o,
                                    _ => unreachable!(),
                                };

                                if obj.contains_key("data") {
                                    let data = ValueMap(match obj.get("data") {
                                        Some(Value::Object(o)) => o.clone(),
                                        _ => unreachable!(),
                                    });
                                    let where_ = ValueMap(match obj.get("where") {
                                        Some(Value::Object(o)) => o.clone(),
                                        _ => unreachable!(),
                                    });

                                    vec.push(NestedValue::Block {
                                        name: name.clone(),
                                        kind: action.clone(),
                                        data,
                                        where_,
                                    });
                                } else {
                                    buf.push(ValueMap(obj.clone()));
                                }
                            }

                            vec.push(NestedValue::Many {
                                name: name.clone(),
                                kind: action.clone(),
                                list: buf,
                            });
                        }
                        Value::Boolean(true) => {
                            vec.push(NestedValue::Simple {
                                name: name.clone(),
                                kind: action.clone(),
                                map: ValueMap::from(&vec![]),
                            });
                        }
                        value => panic!("Unreachable structure: {:?}", value),
                    }
                }
            }
        }

        vec
    }
}
