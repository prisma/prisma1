use connector::ScalarListValues;
use prisma_models::{ManyNodes, PrismaValue, SelectedFields, SingleNode};

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
    pub list_results: ListValues,

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
    pub list_results: ListValues,

    /// Used for filtering implicit fields in result records
    pub selected_fields: SelectedFields,
}

#[derive(Debug)]
pub struct ListValues {
    pub field_names: Vec<String>,
    pub values: Vec<Vec<Vec<PrismaValue>>>,
}

/// This function transforms list results into a presentation that eases the mapping of list results
/// to their corresponding records on higher layers.
/// This is mostly used for result serialization, where we have to combine results into their final representation.
///
/// ```
/// [ // all records
///     [ // one record
///         [ List A ], // one list
///         [ List B ],
///     ],
///     [ // one record
///         [ List A ], // one list
///         [ List B ],
///     ],
///     [ // one record
///         [ List A ], // one list
///         [ List B ],
///     ]
/// ]
/// ```
///
pub fn fold_list_result(list_results: Vec<(String, Vec<ScalarListValues>)>) -> ListValues {
    let field_names: Vec<_> = list_results.iter().map(|(a, _)| a.clone()).collect();

    let values: Vec<Vec<Vec<_>>> = list_results
        .into_iter()
        .map(|(_, vec)| vec.into_iter().map(|s| s.values).collect())
        .collect();

    ListValues { field_names, values }
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
}
