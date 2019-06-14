//! This serves as a skeleton for future capability work.

#[derive(Debug)]
pub struct SupportedCapabilities {
    pub capabilities: Vec<ConnectorCapability>,
}

impl SupportedCapabilities {
    pub fn empty() -> Self {
        SupportedCapabilities { capabilities: vec![] }
    }

    pub fn has(&self, capability: ConnectorCapability) -> bool {
        match capability {
            ConnectorCapability::ScalarLists(l) => self
                .capabilities
                .iter()
                .find(|_| match l {
                    ScalarListsCapability::EmbeddedScalarLists => true,
                    ScalarListsCapability::NonEmbeddedScalarList => true,
                    _ => false,
                })
                .is_some(),
            capability => self.capabilities.iter().find(|c| **c == capability).is_some(),
        }
    }
}

#[allow(dead_code)]
#[derive(Debug, PartialEq)]
pub enum ConnectorCapability {
    ScalarLists(ScalarListsCapability),
    IdCapability(IdCapability),
    EmbeddedTypes,
    JoinRelationsFilter,
    ImportExport,
    TransactionalExecution,
    SupportsExistingDatabases,
    Migrations,
    RawAccess,
    Introspection,
    JoinRelationLinks,
    MongoJoinRelationLinks,
    RelationLinkList,
    RelationLinkTable,
}

#[derive(Debug, PartialEq)]
pub enum ScalarListsCapability {
    ScalarLists, // Not sure if this is required, the scala code is not expressive here.
    EmbeddedScalarLists,
    NonEmbeddedScalarList,
}

#[derive(Debug, PartialEq)]
pub enum IdCapability {
    IntId,
    UuidId,
    IdSequence,
}
