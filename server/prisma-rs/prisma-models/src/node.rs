use crate::{DomainError as Error, DomainResult, GraphqlId, ModelRef, PrismaValue};

#[derive(Debug, Clone)]
pub struct SingleNode {
    pub node: Node,
    pub field_names: Vec<String>,
}

impl SingleNode {
    pub fn get_id_value(&self, model: ModelRef) -> DomainResult<&GraphqlId> {
        let id_field = model.fields().id();
        let index = self
            .field_names
            .iter()
            .position(|r| r == &id_field.name)
            .map(|i| Ok(i))
            .unwrap_or_else(|| {
                Err(Error::FieldNotFound {
                    name: id_field.name.clone(),
                    model: model.name.clone(),
                })
            })?;

        // .expect("did not find value for the id field");
        let value = &self.node.values[index];

        Ok(match value {
            PrismaValue::GraphqlId(ref id) => id,
            _ => unimplemented!(),
        })
    }
}

#[derive(Debug, Clone)]
pub struct ManyNodes {
    pub nodes: Vec<Node>,
    pub field_names: Vec<String>,
}

impl ManyNodes {
    pub fn into_single_node(mut self) -> Option<SingleNode> {
        self.nodes.reverse();
        let node = self.nodes.pop();
        node.map(|n| SingleNode {
            node: n,
            field_names: self.field_names,
        })
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
}

#[derive(Debug, Default, Clone)]
pub struct Node {
    pub values: Vec<PrismaValue>,
    pub related_id: Option<GraphqlId>,
    pub parent_id: Option<GraphqlId>,
}

impl Node {
    pub fn new(values: Vec<PrismaValue>) -> Node {
        Node {
            values,
            ..Default::default()
        }
    }

    pub fn add_related_id(&mut self, related_id: GraphqlId) {
        self.related_id = Some(related_id);
    }

    pub fn add_parent_id(&mut self, parent_id: GraphqlId) {
        self.parent_id = Some(parent_id);
    }
}
