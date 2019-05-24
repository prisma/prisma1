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
pub struct ValueMap(BTreeMap<String, Value>);

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
