use crate::{QueryArguments, ScalarListValues};
use prisma_models::{GraphqlId, ManyRecords, PrismaValue, SingleRecord};

#[derive(Debug)]
pub enum ReadQueryResult {
    Single(SingleReadQueryResult),
    Many(ManyReadQueryResults),
}

impl ReadQueryResult {
    pub fn name(&self) -> String {
        match self {
            ReadQueryResult::Single(s) => s.name.clone(),
            ReadQueryResult::Many(m) => m.name.clone(),
        }
    }
}

#[derive(Debug)]
pub struct SingleReadQueryResult {
    /// Designates the key under which the result is serialized.
    pub name: String,

    /// Holds an ordered list of selected field names.
    pub fields: Vec<String>,

    /// Scalar field results
    pub scalars: Option<SingleRecord>,

    /// Nested queries results
    pub nested: Vec<ReadQueryResult>,

    /// Name of the id field of the contained record.
    pub id_field: String,

    /// Scalar list results, field names mapped to their results
    pub lists: Vec<(String, Vec<ScalarListValues>)>,
}

#[derive(Debug)]
pub struct ManyReadQueryResults {
    /// Designates the key under which all the results are serialized.
    pub name: String,

    /// Holds an ordered list of selected field names for each contained record.
    pub fields: Vec<String>,

    /// Scalar field results
    pub scalars: ManyRecords,

    /// Nested queries results
    pub nested: Vec<ReadQueryResult>,

    /// Scalar list results, field names mapped to their results
    pub lists: Vec<(String, Vec<ScalarListValues>)>,

    /// Required for result processing
    pub query_arguments: QueryArguments,

    /// Name of the id field of the contained records.
    pub id_field: String,

    /// Marker to prohibit explicit struct initialization.
    #[doc(hidden)]
    __inhibit: (),
}

impl SingleReadQueryResult {
    pub fn parent_id(&self) -> Option<&GraphqlId> {
        self.scalars.as_ref().map_or(None, |r| r.record.parent_id.as_ref())
    }

    /// Collect the ID from a record.
    pub fn collect_id(&self) -> Option<&GraphqlId> {
        match &self.scalars {
            Some(ref r) => match r.get_field_value(&self.id_field) {
                Ok(PrismaValue::GraphqlId(ref id)) => Some(id),
                _ => None,
            },
            None => None,
        }
    }
}

impl ManyReadQueryResults {
    pub fn new(
        name: String,
        fields: Vec<String>,
        scalars: ManyRecords,
        nested: Vec<ReadQueryResult>,
        lists: Vec<(String, Vec<ScalarListValues>)>,
        query_arguments: QueryArguments,
        id_field: String,
    ) -> Self {
        let result = Self {
            name,
            fields,
            scalars,
            nested,
            lists,
            query_arguments,
            id_field,
            __inhibit: (),
        };

        // result.remove_excess_records();
        result
    }

    /// Collect all IDs from a query result
    pub fn collect_ids(&self) -> Option<Vec<&GraphqlId>> {
        let id_position: usize = self.scalars.field_names.iter().position(|name| name == "id")?;

        self.scalars
            .records
            .iter()
            .map(|record| record.values.get(id_position))
            .map(|pv| match pv {
                Some(PrismaValue::GraphqlId(id)) => Some(id),
                _ => None,
            })
            .collect()
    }
}
