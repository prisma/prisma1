//! Process a record into an IR Map

use super::{lists::build_list, trim_records, Item, Map};
use connector::{ReadQueryResult, SingleReadQueryResult};
use prisma_models::PrismaValue;

pub fn build_map(result: SingleReadQueryResult) -> Option<Map> {
    // Build selected fields first
    let mut outer = match &result.scalars {
        Some(single) => {
            single
                .field_names
                .iter()
                .zip(&single.record.values)
                .fold(Map::new(), |mut map, (name, val)| {
                    map.insert(name.clone(), Item::Value(val.clone()));
                    map
                })
        }
        None => return None,
    };

    // Parent id for nested queries has to be the id of this record.
    let parent_id = result.find_id().cloned();

    // Then add nested selected fields
    outer = result.nested.into_iter().fold(outer, |mut map, query| {
        match query {
            ReadQueryResult::Single(nested) => {
                let nested_name = nested.name.clone();
                match build_map(nested) {
                    Some(m) => map.insert(nested_name, Item::Map(parent_id.clone(), m)),
                    None => map.insert(nested_name, Item::Value(PrismaValue::Null)),
                }
            }
            ReadQueryResult::Many(nested) => {
                let query_name = nested.name.clone();
                let query_args = nested.query_arguments.clone();
                let mut nested_result = build_list(nested);

                // Trim excess data from the processed result set
                trim_records(&mut nested_result, &query_args);

                map.insert(query_name, Item::List(nested_result))
            }
        };

        map
    });

    // Insert list data into the map
    result.lists.into_iter().for_each(|(field_name, mut list_values)| {
        let values: Vec<PrismaValue> = list_values.pop().into_iter().flat_map(|i| i.values).collect();
        outer.insert(
            field_name,
            Item::List(values.into_iter().map(|i| Item::Value(i)).collect()),
        );
    });

    // Re-order fields to be in-line with what the query specified
    // This also removes implicit fields
    Some(result.fields.iter().fold(Map::new(), |mut map, field| {
        map.insert(
            field.clone(),
            outer.remove(field).expect("[Map]: Missing required field"),
        );
        map
    }))
}
