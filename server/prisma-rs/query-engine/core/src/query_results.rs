use connector::{QueryArguments, ScalarListValues};
use prisma_models::{GraphqlId, ManyNodes, PrismaValue, SelectedFields, SelectedScalarField, SingleNode};

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
    pub name: String,
    pub fields: Vec<String>,

    /// Scalar field results
    pub scalars: Option<SingleNode>,

    /// Nested queries results
    pub nested: Vec<ReadQueryResult>,

    /// Scalar list results, field names mapped to their results
    pub lists: Vec<(String, Vec<ScalarListValues>)>,

    /// Used for filtering implicit fields in result records
    pub selected_fields: SelectedFields,
}

#[derive(Debug)]
pub struct ManyReadQueryResults {
    pub name: String,
    pub fields: Vec<String>,

    /// Scalar field results
    pub scalars: ManyNodes,

    /// Nested queries results
    pub nested: Vec<ReadQueryResult>,

    /// Scalar list results, field names mapped to their results
    pub lists: Vec<(String, Vec<ScalarListValues>)>,

    /// Required for result processing
    pub query_arguments: QueryArguments,

    /// Used for filtering implicit fields in result records
    pub selected_fields: SelectedFields,

    /// Marker to prohibit explicit struct initialization.
    #[doc(hidden)]
    __inhibit: (),
}

impl SingleReadQueryResult {
    /// Returns the implicitly added fields
    #[deprecated]
    #[allow(warnings)]
    pub fn get_implicit_fields(&self) -> Vec<&SelectedScalarField> {
        self.selected_fields.get_implicit_fields()
    }

    pub fn parent_id(&self) -> Option<&GraphqlId> {
        self.scalars.as_ref().map_or(None, |r| r.node.parent_id.as_ref())
    }

    /// Get the ID from a record
    pub fn find_id(&self) -> Option<&GraphqlId> {
        let id_position: usize = self
            .scalars
            .as_ref()
            .map_or(None, |r| r.field_names.iter().position(|name| name == "id"))?;

        self.scalars.as_ref().map_or(None, |r| {
            r.node.values.get(id_position).map(|pv| match pv {
                PrismaValue::GraphqlId(id) => Some(id),
                _ => None,
            })?
        })
    }
}

impl ManyReadQueryResults {
    pub fn new(
        name: String,
        fields: Vec<String>,
        scalars: ManyNodes,
        nested: Vec<ReadQueryResult>,
        lists: Vec<(String, Vec<ScalarListValues>)>,
        query_arguments: QueryArguments,
        selected_fields: SelectedFields,
    ) -> Self {
        let result = Self {
            name,
            fields,
            scalars,
            nested,
            lists,
            query_arguments,
            selected_fields,
            __inhibit: (),
        };

        // result.remove_excess_records();
        result
    }

    /// Returns the implicitly added fields
    #[deprecated]
    #[allow(warnings)]
    pub fn get_implicit_fields(&self) -> Vec<&SelectedScalarField> {
        self.selected_fields.get_implicit_fields()
    }

    /// Get all IDs from a query result
    pub fn find_ids(&self) -> Option<Vec<&GraphqlId>> {
        let id_position: usize = self.scalars.field_names.iter().position(|name| name == "id")?;
        self.scalars
            .nodes
            .iter()
            .map(|node| node.values.get(id_position))
            .map(|pv| match pv {
                Some(PrismaValue::GraphqlId(id)) => Some(id),
                _ => None,
            })
            .collect()
    }
}
