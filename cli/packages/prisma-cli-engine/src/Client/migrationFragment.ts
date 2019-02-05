const RELATION_MANIFESTATION_FRAGMENT = `
  type
  ... on Inline {
    model
    column
  }

  ... on LinkTable {
    table
    modelAColumn
    modelBColumn
    idColumn
    type
  }
`

/**
 * Generated a graphql fragment for the step type of the management api
 * @param r renderMigrationManifestations
 */
export const renderStepFragment = (r: boolean) => `
  type
  __typename
  ... on CreateEnum {
    name
    ce_values: values
  }
  ... on CreateField {
    model
    name
    cf_typeName: typeName
    cf_isRequired: isRequired
    cf_isList: isList
    cf_isUnique: unique
    cf_relation: relation
    cf_defaultValue: default
    cf_enum: enum
  }
  ... on CreateModel {
    name
  }
  ... on CreateRelation {
    name
    leftModel
    rightModel
    ${r ? `after { ${RELATION_MANIFESTATION_FRAGMENT} }` : ''}
  }
  ${
    r
      ? `
    ... on UpdateRelation {
      ur_name: name
      ur_newName: newName
      before { ${RELATION_MANIFESTATION_FRAGMENT} }
      ur_after: after { ${RELATION_MANIFESTATION_FRAGMENT} }
    }
  `
      : ''
  }
  ... on DeleteEnum {
    name
  }
  ... on DeleteField {
    model
    name
  }
  ... on DeleteModel {
    name
  }
  ... on DeleteRelation {
    name
  }
  ... on UpdateEnum {
    name
    newName
    values
  }
  ... on UpdateField {
    model
    name
    newName
    typeName
    isRequired
    isList
    isUnique: unique
    relation
    default
    enum
  }
  ... on UpdateModel {
    name
    um_newName: newName
  }
`

export const renderMigrationFragment = (
  renderRelationManifestations: boolean,
) => `
  fragment MigrationFragment on Migration {
    revision
    steps {
      ${renderStepFragment(renderRelationManifestations)}
    }
  }
`
