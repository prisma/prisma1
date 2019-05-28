//!
#![allow(warnings)]

/*
data: {
    Name: "Cool Artist"
    Albums: {
        create: {
            Title: "Super Cool Album"
            Tracks: {
                create: {
                    Name: "Cool Song"
                    Milliseconds: 9001
                }
            }
            Aliases: { set: [ "Awesome", "Gnarly" ] }
        }
    }
    Friends: {
        upsert: {
            where: {},
            create: {},
            update: {},
        }
    }
    Aliases: { set: [ "Bob" ] }
}
*/

use graphql_parser::query::Value;
use prisma_models::PrismaValue;
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
                Some(vec) => Some(vec.into_iter().map(|v| PrismaValue::from_value(&v)).collect()),
                None => None,
            },
        )
    }
}

impl ValueMap {
    pub fn init(from: &Vec<(String, Value)>) -> Self {
        Self(from.into_iter().map(|k| k.clone()).collect())
    }

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
            .map(|(k, v)| (k, PrismaValue::from_value(&v)))
            .collect()
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
    Connect {
        name: String,
        list: Vec<ValueMap>,
    },
    Upsert {
        name: String,
        create: ValueMap,
        update: ValueMap,
    },
}

impl ValueMap {
    /// Extract mutation arguments from a value map
    pub fn eval_tree(&self) -> Vec<NestedValue> {
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
                vec.push(match nested {
                    Value::Object(obj) => NestedValue::Simple {
                        name: name.clone(),
                        kind: action.clone(),
                        map: ValueMap(obj.clone()),
                    },
                    Value::List(list) => NestedValue::Connect {
                        name: name.clone(),
                        list: list
                            .iter()
                            .map(|item| match item {
                                Value::Object(obj) => ValueMap(obj.clone()),
                                _ => unreachable!(),
                            })
                            .collect(),
                    },
                    _ => unreachable!(),
                });

                // vec.push(NestedValue::Simple {
                //     name: name.clone(),
                //     kind: action.clone(),
                //     map: match dbg!(nested) {
                //         Value::Object(obj) => ValueMap(obj.clone()),
                //         Value::List(value) => ValueMap::init(
                //             &value
                //                 .iter()
                //                 .zip(0..)
                //                 .fold(vec![], |mut vec, (item, n)| {
                //                     vec.push((format!("{}_{}", action, n), item.clone()));
                //                     vec
                //                 })),
                //         _ => unreachable!()
                //         // Value::List(list) => ValueMap::init(&list.iter().map(|v| (name.clone(), v.clone())).collect()),
                //         // value => ValueMap::init(&vec![(name.clone(), value.clone())]),
                //     },
                // });
            }
        }

        vec
    }
}
