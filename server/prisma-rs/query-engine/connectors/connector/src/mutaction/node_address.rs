use crate::{filter::NodeSelector, mutaction::Path};

pub struct NodeAddress {
    pub path: Path,
    pub node_selector: NodeSelector,
}
