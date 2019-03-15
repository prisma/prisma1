use crate::{GraphqlId, PrismaValue};

#[derive(Debug)]
pub struct SingleNode {
    pub node: Node,
    pub field_names: Vec<String>,
}

#[derive(Debug)]
pub struct ManyNodes {
    pub nodes: Vec<Node>,
    pub field_names: Vec<String>,
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
