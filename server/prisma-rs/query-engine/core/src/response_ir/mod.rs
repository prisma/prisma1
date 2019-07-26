//! Prisma Response (Intermediate Data Representation)
//!
//! This module takes care of processing the results
//! and transforming them into a different AST.
//!
//! This IR is meant for general processing and storage.
//! It can also be easily serialized.

// mod lists;
// mod maps;

use crate::{CoreError, CoreResult, IntoArc, ObjectTypeStrongRef, OutputType, OutputTypeRef, ResultPair};
use connector::{QueryArguments, QueryResult, ReadQueryResult, WriteQueryResult};
use indexmap::IndexMap;
use prisma_models::{GraphqlId, PrismaValue};
use std::borrow::Borrow;

/// A `key -> value` map to an IR item
pub type Map = IndexMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// A response can either be some `key-value` data representation
/// or an error that occured.
#[derive(Debug)]
pub enum Response {
    /// A data item has a name it will be returned under, and and actual item.
    Data(String, Item),
    Error(String), // FIXME: Use actual error type
}

/// An IR item that either expands to a subtype or leaf-record
#[derive(Debug)]
pub enum Item {
    /// (Parent ID, transformed record as map)
    Map(Option<GraphqlId>, Map),
    List(List),
    Value(PrismaValue),
}

/// An IR builder utility
#[derive(Debug)]
pub struct ResultIrBuilder(Vec<ResultPair>);

impl ResultIrBuilder {
    pub fn new() -> Self {
        Self(vec![])
    }

    /// Add a single query result to the builder
    pub fn add(mut self, q: ResultPair) -> Self {
        self.0.push(q);
        self
    }

    /// Parse collected queries into the return wrapper type
    pub fn build(self) -> CoreResult<Vec<Response>> {
        self.0
            .into_iter()
            .fold(vec![], |mut vec, res| {
                let response = match res {
                    ResultPair::Read(r, typ) => Self::serialize_read(r, typ), // todo coerce into error types on err
                    _ => unimplemented!(),
                };

                vec.push(response);
                vec
            })
            .into_iter()
            .collect()

        // self.0.into_iter().fold(vec![], |mut vec, res| {
        //     vec.push(match res {
        //         QueryResult::Read(ReadQueryResult::Single(query)) => {
        //             let query_name = query.name.clone();
        //             match maps::build_map(query) {
        //                 Some(m) => Response::Data(query_name, Item::Map(None, m)),
        //                 None => Response::Data(query_name, Item::Value(PrismaValue::Null)),
        //             }
        //         }
        //         QueryResult::Read(ReadQueryResult::Many(query)) => {
        //             let query_name = query.name.clone();
        //             let query_args = query.query_arguments.clone();
        //             let mut result = lists::build_list(query);

        //             // Trim excess data from the processed result set
        //             trim_records(&mut result, &query_args);
        //             Response::Data(query_name, Item::List(result))
        //         }
        //         QueryResult::Direct(resp) => resp,
        //     });

        //     vec
        // })
    }

    /// The query validation makes sure that the output selection already has the correct shape.
    /// This means that we can make the following assumptions:
    /// - Objects don't need to check required fields.
    /// - Objects don't need to check extra fields - just pick the selected ones and ignore the rest.
    ///
    ///
    /// The output validation has to make sure that returned values:
    /// - Are of the correct type.
    /// - Are nullable if not present.
    fn serialize_read(result: ReadQueryResult, typ: OutputTypeRef) -> CoreResult<Response> {
        let name = result.alias.clone().unwrap_or_else(|| result.name.clone());

        // Depth-first serialization
        // First, move lists and nested out of result for separate processing.
        // let nested = std::mem::replace(&mut result.nested, vec![]);
        // let lists = std::mem::replace(&mut result.lists, vec![]);

        // // Parse and validate all nested objects with their respective output type
        // nested.into_iter().map(|nested_result| {
        //     // Returns field name -> Item?
        //     // todo...
        // });

        let item: Option<Item> = match typ.borrow() {
            OutputType::Object(obj) => {
                Self::serialize_object(result, obj.into_arc())? //.map(|res| res.map(|res| Item::Map(None, res)))
            }

            OutputType::List(inner) => unimplemented!(), // trim here?
            OutputType::Opt(inner) => {
                // let inner_result =
                unimplemented!()
            },
            OutputType::Enum(et) => unimplemented!(),
            OutputType::Scalar(st) => unimplemented!(),
        };

        // The optional handling above makes sure that if a field is nullable, it's returned as PrismaValue::Null.
        match item {
            Some(item) => Response::Data(name, item),
            None => Err(CoreError::SerializationError(format!("Required field {} returned null.", name))),
        }
    }

    /// None if no list is present
    fn serialize_list(result: ReadQueryResult, typ: OutputTypeRef) -> CoreResult<Option<List>> {
        unimplemented!()
    }

    /// None if no object is present
    fn serialize_object(result: ReadQueryResult, typ: ObjectTypeStrongRef) -> CoreResult<Option<Item>> {
        // Match fields to

        unimplemented!()
    }

    fn serialize_scalar(result: ReadQueryResult, typ: ObjectTypeStrongRef) -> CoreResult<PrismaValue> {
        unimplemented!()
    }

    fn serialize_enum(result: ReadQueryResult, typ: ObjectTypeStrongRef) -> CoreResult<Map> {
        unimplemented!()
    }
}

// /// Attempts to coerce the given write result into the provided output type.
// fn coerce_result(result: WriteQueryResult, typ: &OutputTypeRef) -> CoreResult<QueryResult> {
//     let value: PrismaValue = match result.identifier {
//         Identifier::Id(id) => id.into(),
//         Identifier::Count(c) => PrismaValue::from(c), // Requires object with one field that is usize / int / float, or single scalar type.
//         Identifier::Record(r) => unimplemented!(),    // Requires object. Try coercing all fields of the object.
//         Identifier::None => unimplemented!(),         // Null?
//     };

//     unimplemented!()
// }

// fn coerce_value_type(val: PrismaValue, typ: &OutputTypeRef) -> CoreResult<()> {
//     match typ.borrow() {
//         OutputType::Object(o) => unimplemented!(),
//         OutputType::Opt(inner) => unimplemented!(),
//         OutputType::Enum(e) => unimplemented!(),
//         OutputType::List(inner) => unimplemented!(),
//         OutputType::Scalar(s) => unimplemented!(),
//     };

//     unimplemented!()
// }

// fn coerce_scalar() -> CoreResult<()> {
//     unimplemented!()
// }

/// Removes the excess records added to by the database query layer based on the query arguments
/// This would be the right place to add pagination markers (has next page, etc.).
pub fn trim_records(data: &mut Vec<Item>, query_args: &QueryArguments) {
    // The query engine reverses lists when querying for `last`, so we need to reverse again to have the intended order.
    if query_args.last.is_some() {
        data.reverse();
    }

    match (query_args.first, query_args.last) {
        (Some(f), _) if data.len() > f as usize => drop_right(data, 1),
        (_, Some(l)) if data.len() > l as usize => drop_left(data, 1),
        _ => (),
    };
}

/// Drops x records on the end of the wrapped records in place.
fn drop_right<T>(vec: &mut Vec<T>, x: u32) {
    vec.truncate(vec.len() - x as usize);
}

/// Drops x records on the start of the wrapped records in place.
fn drop_left<T>(vec: &mut Vec<T>, x: u32) {
    vec.reverse();
    drop_right(vec, x);
    vec.reverse();
}
