//! Process a set of records into an IR List

use super::{maps::build_map, utils, Item, List, Map};
use crate::{ManyReadQueryResults, ReadQueryResult};

pub fn build_list(result: &ManyReadQueryResults) -> List {
    let mut vec: Vec<Item> = result
        .scalars
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
                ReadQueryResult::Single(nested) => map.insert(nested.name.clone(), Item::Map(build_map(nested))),
                ReadQueryResult::Many(nested) => map.insert(nested.name.clone(), Item::List(build_list(nested))),
            },
            _ => unreachable!(),
        };
    });

    // Explicitly handle scalar-list results
    let ids = result.find_ids().expect("Failed to find record IDs!");
    let scalar_values = utils::associate_list_results(ids, &result.lists);

    // Then just merge the maps into the existing data
    vec = vec.into_iter().zip(scalar_values).fold(vec![], |mut vec, iter| {
        vec.push(Item::Map(match iter {
            (Item::Map(map), Item::Map(scalars)) => scalars.into_iter().fold(map, |mut map, (k, v)| {
                map.insert(k, v);
                map
            }),
            _ => unreachable!("Tried merging two `Item`s that were not `Map`"),
        }));

        vec
    });

    // Re-order fields to be in-line with what the query specified
    // This also removes implicit fields
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
