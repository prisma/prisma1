//! Provides scoped arguments for mutations

#![allow(warnings)]

use std::collections::BTreeMap;
use prisma_models::PrismaValue;
use graphql_parser::query::Value;

/// Scoped arguments are either leafs or branches
pub enum ScopedArg<'name> {
    Node(ScopedArgNode<'name>),
    Value(PrismaValue)
}

type ListArg<'name> = (String, Option<Vec<ScopedArg<'name>>>);

/// A node that holds some mutation arguments
#[derive(Default)]
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

impl<'name> ScopedArg<'name> {
    /// Parse a set of GraphQl input arguments
    pub fn parse(args: &'name Vec<(String, Value)>) -> Self {
        args
            .iter()
            .filter(|(arg, _)| arg.as_str() != "where")
            .fold(ScopedArg::Node(Default::default()), |mut node, (name, data)| {
                match (name.as_str(), data) {
                    ("data", Value::Object(obj)) => {},
                    ("set", Value::Object(obj)) => {},
                    ("create", Value::Object(obj)) => {},
                    ("connect", Value::Object(obj)) => {},
                    _ => { /* ignore - maybe log? */ }
                }

                match node {
                    ScopedArg::Node(ref mut n) => n.name = name.as_str(),
                    _ => {}
                };

                node
            })
    }

    fn evaluate_root(args: &'name Vec<(String, Value)>) {
        args.iter().for_each(|(name, value)| {
            match (name.as_str(), value) {
                ("data", Value::Object(obj)) => {
                    let s: ScopedArgNode = obj.iter().fold(Default::default(), |mut node, (key, value)| {
                        match value {
                            // Handle scalar-list arguments
                            Value::Object(obj) if obj.contains_key("set") => {
                                node.lists.push(handle_scalar_list(&key, obj));
                            },
                            // Handle nested arguments
                            Value::Object(obj) => {
                                node.data.insert(key.clone(), Self::evaluate_tree(key.as_str(), obj));
                            },
                            // Single data scalars
                            value => {
                                node.data.insert(key.clone(), ScopedArg::Value(PrismaValue::from_value(value)));
                            }
                        }

                        node
                    });
                },
                _ => unreachable!(),
            }
        });
    }

    /// Determine whether a subtree needs to be expanded into it's own node
    fn evaluate_tree(name: &'name str, obj: &BTreeMap<String, Value>) -> Self {
        unimplemented!()
    }
}

/// Parse a `{ "set": [...] }` structure into a ScalarListSet
fn handle_scalar_list<'name>(name: &String, obj: &'name BTreeMap<String, Value>) -> ListArg<'name> {
    // match obj.get("set") {
    //     Some(Value::List(l)) => Some(l.iter().map(|v| ScopedArg::Value(PrismaValue::from_value(v))).collect::<ListArg<'name>>()),
    //     None => None,
    //     _ => unimplemented!(), // or unreachable? dunn duuuuun!
    // }

    unimplemented!()
}



//    let (args, lists) = dbg!(args)
//         .iter()
//         .filter(|(arg, _)| arg.as_str() != "where") // `where` blocks are handled by filter logic!
//         .fold((BTreeMap::new(), vec![]), |(mut map, mut vec), (_, v)| {
//             match v {
//                 Value::Object(o) => o.iter().for_each(|(k, v)| {
//                     match v {
//                         // Deal with ScalarList initialisers
//                         Value::Object(o) if o.contains_key("set") => {
//                             vec.push((
//                                 k.clone(),
//                                 match o.get("set") {
//                                     Some(Value::List(l)) => Some(
//                                         l.iter()
//                                             .map(|v| PrismaValue::from_value(v))
//                                             .collect::<Vec<PrismaValue>>(),
//                                     ),
//                                     None => None,
//                                     _ => unimplemented!(), // or unreachable? dunn duuuuun!
//                                 },
//                             ));
//                         },
//                         // Deal with nested creates
//                         Value::Object(o) if o.contains_key("create") => {

//                         },
//                         // Deal with nested connects
//                         Value::Object(o) if o.contains_key("connect") => {

//                         }
//                         v => {
//                             map.insert(k.clone(), PrismaValue::from_value(v));
//                         }
//                     }
//                 }),
//                 _ => panic!("Unknown argument structure!"),
//             }

//             (map, vec)
//         });
//     (args.into(), lists)

// arguments: [
//     (
//         "data",
//         Object(
//             {
//                 "Albums": Object(
//                     {
//                         "create": Object(
//                             {
//                                 "Title": String(
//                                     "Anarchy",
//                                 ),
//                                 "Tracks": Object(
//                                     {
//                                         "create": Object(
//                                             {
//                                                 "MediaType": Object(
//                                                     {
//                                                         "connect": Object(
//                                                             {
//                                                                 "id": Int(
//                                                                     Number(
//                                                                         1,
//                                                                     ),
//                                                                 ),
//                                                             },
//                                                         ),
//                                                     },
//                                                 ),
//                                                 "Milliseconds": Int(
//                                                     Number(
//                                                         1312,
//                                                     ),
//                                                 ),
//                                                 "Name": String(
//                                                     "Destroy Capitalism",
//                                                 ),
//                                                 "UnitPrice": Float(
//                                                     13.12,
//                                                 ),
//                                             },
//                                         ),
//                                     },
//                                 ),
//                             },
//                         ),
//                     },
//                 ),
//                 "Name": String(
//                     "The ACAB tribute band",
//                 ),
//             },
//         ),
//     ),
// ],