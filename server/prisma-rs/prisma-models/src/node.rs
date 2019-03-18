use crate::{GraphqlId, ModelRef, PrismaValue};

#[derive(Debug)]
pub struct SingleNode {
    pub node: Node,
    pub field_names: Vec<String>,
}
impl SingleNode {
    pub fn get_id_value(&self, model: ModelRef) -> &GraphqlId {
        let id_field = model.fields().id();
        let index = self
            .field_names
            .iter()
            .position(|r| r == &id_field.name)
            .expect("did not find value for the id field");
        let value = &self.node.values[index];
        match value {
            PrismaValue::GraphqlId(ref id) => id,
            _ => unimplemented!(),
        }
    }
}

#[derive(Debug)]
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
}

#[derive(Debug, Default)]
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
