use connector::ScalarListValues;
use prisma_models::{ManyNodes, PrismaValue, SelectedFields, SingleNode, GraphqlId};

#[derive(Debug)]
pub enum ReadQueryResult {
    Single(SingleReadQueryResult),
    Many(ManyReadQueryResults),
}

#[derive(Debug)]
pub struct SingleReadQueryResult {
    pub name: String,
    pub fields: Vec<String>,
    pub result: Option<SingleNode>,
    pub nested: Vec<ReadQueryResult>,

    /// Scalar list field names mapped to their results
    pub list_results: Vec<(String, Vec<ScalarListValues>)>,

    /// Used for filtering implicit fields in result records
    pub selected_fields: SelectedFields,
}

#[derive(Debug)]
pub struct ManyReadQueryResults {
    pub name: String,
    pub fields: Vec<String>,
    pub result: ManyNodes,
    pub nested: Vec<ReadQueryResult>,

    /// Scalar list field names mapped to their results
    pub list_results: Vec<(String, Vec<ScalarListValues>)>,

    /// Used for filtering implicit fields in result records
    pub selected_fields: SelectedFields,
}

impl ReadQueryResult {
    /// Filters implicitly selected fields from the result set.
    pub fn filter(self) -> Self {
        match self {
            ReadQueryResult::Single(s) => ReadQueryResult::Single(s.filter()),
            ReadQueryResult::Many(m) => ReadQueryResult::Many(m.filter()),
        }
    }
}

// Q: Best pattern here? Mix of in place mutation and recreating result
impl SingleReadQueryResult {
    /// Filters implicitly selected fields in-place in the result record and field names.
    /// Traverses nested result tree.
    pub fn filter(self) -> Self {
        let implicit_fields = self.selected_fields.get_implicit_fields();

        let result = self.result.map(|mut r| {
            let positions: Vec<usize> = implicit_fields
                .into_iter()
                .filter_map(|implicit| r.field_names.iter().position(|name| &implicit.field.name == name))
                .collect();

            positions.into_iter().for_each(|p| {
                r.field_names.remove(p);
                r.node.values.remove(p);
            });

            r
        });

        let nested = self.nested.into_iter().map(|nested| nested.filter()).collect();

        Self { result, nested, ..self }
    }

    /// Get the ID from a record
    pub fn find_id(&self) -> Option<&GraphqlId> {
        let id_position: usize = self
            .result
            .as_ref()
            .map_or(None, |r| r.field_names.iter().position(|name| name == "id"))?;

        self.result.as_ref().map_or(None, |r| {
            r.node.values.get(id_position).map(|pv| match pv {
                PrismaValue::GraphqlId(id) => Some(id),
                _ => None,
            })?
        })
    }
}

impl ManyReadQueryResults {
    /// Filters implicitly selected fields in-place in the result records and field names.
    /// Traverses nested result tree.
    pub fn filter(mut self) -> Self {
        let implicit_fields = self.selected_fields.get_implicit_fields();
        let positions: Vec<usize> = implicit_fields
            .into_iter()
            .filter_map(|implicit| {
                self.result
                    .field_names
                    .iter()
                    .position(|name| &implicit.field.name == name)
            })
            .collect();

        positions.iter().for_each(|p| {
            self.result.field_names.remove(p.clone());
        });

        // Remove values on found positions from all records.
        let records = self
            .result
            .nodes
            .into_iter()
            .map(|mut record| {
                positions.iter().for_each(|p| {
                    record.values.remove(p.clone());
                });
                record
            })
            .collect();

        let result = ManyNodes {
            nodes: records,
            ..self.result
        };
        let nested = self.nested.into_iter().map(|nested| nested.filter()).collect();

        Self { result, nested, ..self }
    }

    /// Get all IDs from a query result
    pub fn find_ids(&self) -> Option<Vec<&GraphqlId>> {
        let id_position: usize = self.result.field_names.iter().position(|name| name == "id")?;
        self.result
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
