use crate::{DomainError as Error, DomainResult, GraphqlId, ModelRef, PrismaValue};
use std::{convert::TryFrom, sync::Arc};

#[derive(Debug, Clone)]
pub struct SingleNode {
    pub node: Node,
    pub field_names: Vec<String>,
}

impl TryFrom<ManyNodes> for SingleNode {
    type Error = Error;

    fn try_from(mn: ManyNodes) -> DomainResult<SingleNode> {
        let field_names = mn.field_names;

        mn.nodes
            .into_iter()
            .rev()
            .next()
            .map(|node| SingleNode { node, field_names })
            .ok_or(Error::ConversionFailure("ManyNodes", "SingleNode"))
    }
}

impl SingleNode {
    pub fn new(node: Node, field_names: Vec<String>) -> Self {
        Self { node, field_names }
    }

    pub fn get_id_value(&self, model: ModelRef) -> DomainResult<&GraphqlId> {
        self.node.get_id_value(&self.field_names, model)
    }

    pub fn get_field_value(&self, field: &str) -> DomainResult<&PrismaValue> {
        self.node.get_field_value(&self.field_names, field)
    }
}

#[derive(Debug, Clone)]
pub struct ManyNodes {
    pub nodes: Vec<Node>,
    pub field_names: Vec<String>,
}

impl ManyNodes {
    pub fn get_id_values(&self, model: ModelRef) -> DomainResult<Vec<GraphqlId>> {
        self.nodes
            .iter()
            .map(|node| {
                node.get_id_value(&self.field_names, Arc::clone(&model))
                    .map(|i| i.clone())
            })
            .collect()
    }

    /// Maps into a Vector of (field_name, value) tuples
    pub fn as_pairs(&self) -> Vec<Vec<(String, PrismaValue)>> {
        self.nodes
            .iter()
            .map(|node| {
                node.values
                    .iter()
                    .zip(self.field_names.iter())
                    .map(|(value, name)| (name.clone(), value.clone()))
                    .collect()
            })
            .collect()
    }

    /// Reverses the wrapped records in place
    pub fn reverse(&mut self) {
        self.nodes.reverse();
    }
}

#[derive(Debug, Default, Clone)]
pub struct Node {
    pub values: Vec<PrismaValue>,
    pub parent_id: Option<GraphqlId>,
}

impl Node {
    pub fn new(values: Vec<PrismaValue>) -> Node {
        Node {
            values,
            ..Default::default()
        }
    }

    pub fn get_id_value(&self, field_names: &Vec<String>, model: ModelRef) -> DomainResult<&GraphqlId> {
        let id_field = model.fields().id();
        let index = field_names
            .iter()
            .position(|r| r == &id_field.name)
            .map(|i| Ok(i))
            .unwrap_or_else(|| {
                Err(Error::FieldNotFound {
                    name: id_field.name.clone(),
                    model: model.name.clone(),
                })
            })?;

        Ok(match &self.values[index] {
            PrismaValue::GraphqlId(ref id) => id,
            _ => unimplemented!(),
        })
    }

    pub fn get_field_value(&self, field_names: &Vec<String>, field: &str) -> DomainResult<&PrismaValue> {
        let index = field_names
            .iter()
            .position(|r| r == field)
            .map(|i| Ok(i))
            .unwrap_or_else(|| {
                Err(Error::FieldNotFound {
                    name: field.to_string(),
                    model: String::new(),
                })
            })?;

        Ok(&self.values[index])
    }

    /// (WIP) Associate a nested selection-set with a set of parents
    ///
    /// - A parent is a `ManyNodes` which has selected fields and nested queries.
    /// - Nested queries aren't associated to a parent, but have a `parent_id` and `related_id`
    /// - This function takes the parent query and creates a set of `(String, PrismaValue)` for each query
    /// - Returns a nested vector of tuples
    ///   - List item for every query in parent
    ///   - Then a vector of selected fields in each nested query
    ///   - Actual association is made via `get_pairs` to `(String, PrismaValue)`
    ///
    pub fn get_parent_pairs(
        &self,
        parent: &ManyNodes,
        selected_fields: &Vec<String>,
    ) -> Vec<Vec<(String, PrismaValue)>> {
        parent.nodes.iter().fold(Vec::new(), |mut vec, _parent| {
            vec.push(
                self.values
                    .iter()
                    .zip(selected_fields)
                    .fold(Vec::new(), |vec, (_value, _field)| vec),
            );
            vec
        })
    }

    pub fn add_parent_id(&mut self, parent_id: GraphqlId) {
        self.parent_id = Some(parent_id);
    }
}
