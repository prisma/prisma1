import { getSchemaPathFromConfig } from './getSchemaPathFromConfig'

describe('getSchemaPathFromConfig', () => {
  test('graphqlconfig config with prisma project in toplevel', () => {
    const schemaPath = getSchemaPathFromConfig(
      __dirname + '/fixtures/toplevel-config',
    )
    expect(schemaPath).toBe('toplevel-schema.graphql')
  })
  test('graphqlconfig config with prisma project not called database', () => {
    const schemaPath = getSchemaPathFromConfig(
      __dirname + '/fixtures/project-config',
    )
    expect(schemaPath).toBe('my-project-schema.graphql')
  })
})
