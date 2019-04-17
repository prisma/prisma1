//! Process a set of records into an IR List

use crate::{MultiPrismaQueryResult, PrismaQueryResult};
use super::{List, Map, Item, maps::build_map};
use itertools::{Itertools, EitherOrBoth::{Both, Left}};

pub fn build_list(result: &MultiPrismaQueryResult) -> List {
    let mut vec: Vec<Item> = result
        .result
        .as_pairs()
        .iter()
        .map(|vec| {
            Item::Map(vec.iter().fold(Map::new(), |mut map, (name, value)| {
                map.insert(name.clone(), Item::Value(value.clone()));
                map
            }))
        })
        .collect();

    result.nested.iter().zip(&mut vec).for_each(|(nested, map)| {
        match map {
            Item::Map(ref mut map) => match nested {
                PrismaQueryResult::Single(nested) => map.insert(nested.name.clone(), Item::Map(build_map(nested))),
                PrismaQueryResult::Multi(nested) => map.insert(nested.name.clone(), Item::List(build_list(nested))),
            },
            _ => unreachable!(),
        };
    });

    dbg!(&result.list_results);
    dbg!(&vec);

    // Associate scalarlists with corresponding records
    //
    // This is done by iterating through both existing records and the list of
    // list-results. But because the list-results list can be shorter, we need
    // to zip_longest() which yields a special enum. We differentiate between
    // "only record was found" and "both record and corresponding list data
    // was found". In case there's only list data but no record, we panic.
    vec.iter_mut().zip_longest(&result.list_results.values).for_each(|eob| {
        match eob {

            Both(item, values) => {
                if let Item::Map(ref mut map) = item {
                    values
                        .iter()
                        .zip(&result.list_results.field_names)
                        .for_each(|(list, field_name)| {
                            map.insert(
                                field_name.clone(),
                                Item::List(list.iter().map(|pv| Item::Value(pv.clone())).collect()),
                            );
                        })
                }
            }
            Left(item) => {
                if let Item::Map(ref mut map) = item {
                    result.list_results.field_names.iter().for_each(|field_name| {
                        map.insert(field_name.clone(), Item::List(vec![]));
                    })
                }
            }
            _ => unreachable!("Unexpected scalar-list values for missing record")
        };
    });


    vec.into_iter()
        .fold(vec![], |mut vec, mut item| {
            if let Item::Map(ref mut map) = item {
                vec.push(result.fields.iter().fold(Map::new(), |mut new, field| {
                    let item = map.remove(field).expect("[List]: Missing required field");
                    new.insert(field.clone(), item);
                    new
                }));
            }

            vec
        })
        .into_iter()
        .map(|i| Item::Map(i))
        .collect()
}
