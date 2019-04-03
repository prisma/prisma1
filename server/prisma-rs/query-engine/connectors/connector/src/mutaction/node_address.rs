use crate::{Path, NodeSelector};

pub struct NodeAddress {
    pub path: Path,
    pub node_selector: NodeSelector,
}
