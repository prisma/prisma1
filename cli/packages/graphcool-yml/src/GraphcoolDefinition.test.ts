import { GraphcoolDefinitionClass } from './GraphcoolDefinition'
import * as fs from 'fs-extra'
import * as path from 'path'
import { getTmpDir } from './test/getTmpDir'
import { Environment } from './Environment'
import { makeEnv } from './Environment.test'
import { Args } from './types/common'

const defaultGlobalRC = `graphcool-1.0:
  clusters:
    local:
      host: 'http://localhost:60000'
    remote:
      host: 'https://remote.graph.cool'
      clusterSecret: 'here-is-a-token'
`

function makeDefinition(
  yml: string,
  datamodel: string,
  args: Args = {},
  globalRC: string = defaultGlobalRC,
) {
  const definitionDir = getTmpDir()
  const definitionPath = path.join(definitionDir, 'graphcool.yml')
  const modelPath = path.join(definitionDir, 'datamodel.graphql')
  const env = makeEnv(defaultGlobalRC)

  const definition = new GraphcoolDefinitionClass(env, definitionPath)

  fs.writeFileSync(modelPath, datamodel)
  fs.writeFileSync(definitionPath, yml)

  return { env, definition }
}

async function loadDefinition(
  yml: string,
  datamodel: string,
  args: Args = {},
  envPath?: string,
  globalRC: string = defaultGlobalRC,
) {
  const { env, definition } = makeDefinition(yml, datamodel, args, globalRC)
  await env.load(args)
  await definition.load(args, envPath)
  return { env, definition }
}

describe('graphcool definition', () => {
  test('load basic yml, provide cluster', async () => {
    const yml = `\
service: jj
stage: dev
cluster: local

datamodel:
- datamodel.graphql

secret: some-secret

schema: schemas/database.graphql
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`

    const { definition } = await loadDefinition(yml, datamodel)

    expect(definition.definition).toMatchSnapshot()
    expect(definition.getCluster()).toMatchSnapshot()
  })
  test('load yml with secret and env var', async () => {
    const secret = 'this-is-a-long-secret'
    process.env.MY_TEST_SECRET = secret
    const yml = `\
service: jj
stage: dev
cluster: local

datamodel:
- datamodel.graphql

secret: \${env:MY_TEST_SECRET}

schema: schemas/database.graphql
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`

    const { definition } = await loadDefinition(yml, datamodel)

    expect(definition.definition).toMatchSnapshot()
  })
  test('load yml with secret and env var in .env', async () => {
    const secret = 'this-is-a-long-secret'
    const yml = `\
service: jj
stage: dev
cluster: local

datamodel:
- datamodel.graphql

secret: \${env:MY_DOT_ENV_SECRET}

schema: schemas/database.graphql
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`
    const { definition, env } = makeDefinition(yml, datamodel, {})
    const envPath = path.join(definition.definitionDir, '.env')

    fs.writeFileSync(
      envPath,
      `MY_DOT_ENV_SECRET=this-is-very-secret,and-comma,seperated`,
    )

    await env.load({})
    await definition.load({}, envPath)

    expect(definition.definition).toMatchSnapshot()
    expect(definition.secrets).toMatchSnapshot()
  })
  /**
   * This test ensures, that GRAPHCOOL_SECRET can't be injected anymore
   */
  test('dont load yml with secret and env var in args', async () => {
    const yml = `\
service: jj
stage: dev
cluster: local

datamodel:
- datamodel.graphql

schema: schemas/database.graphql
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`

    const definitionDir = getTmpDir()
    const definitionPath = path.join(definitionDir, 'graphcool.yml')
    const modelPath = path.join(definitionDir, 'datamodel.graphql')
    const env = makeEnv(defaultGlobalRC)

    const definition = new GraphcoolDefinitionClass(env, definitionPath, {
      GRAPHCOOL_SECRET: 'this-is-secret',
    })

    fs.writeFileSync(modelPath, datamodel)
    fs.writeFileSync(definitionPath, yml)

    let error
    try {
      await env.load({})
      await definition.load({})
    } catch (e) {
      error = e
    }

    expect(error).toMatchSnapshot()
  })
  test('load yml with disableAuth: true', async () => {
    const secret = 'this-is-a-long-secret'
    const yml = `\
service: jj
stage: dev
cluster: local

datamodel:
- datamodel.graphql

disableAuth: true

schema: schemas/database.graphql
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`
    const { definition, env } = makeDefinition(yml, datamodel)
    const envPath = path.join(definition.definitionDir, '.env')

    fs.writeFileSync(
      envPath,
      `MY_DOT_ENV_SECRET=this-is-very-secret,and-comma,seperated`,
    )

    await env.load({})
    await definition.load({}, envPath)

    expect(definition.definition).toMatchSnapshot()
    expect(definition.secrets).toMatchSnapshot()
  })
  test('throw when no secret or disable auth provided', async () => {
    const yml = `\
service: jj
stage: dev
cluster: local

datamodel:
- datamodel.graphql

schema: schemas/database.graphql
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`

    let error
    try {
      const { definition } = await loadDefinition(yml, datamodel)
    } catch (e) {
      error = e
    }

    expect(error).toMatchSnapshot()
  })
  test('throws when stages key apparent', async () => {
    const yml = `\
service: jj
stage: dev
cluster: local

datamodel:
- datamodel.graphql

schema: schemas/database.graphql

stages:
  dev: local
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`

    let error
    try {
      const { definition } = await loadDefinition(yml, datamodel)
    } catch (e) {
      error = e
    }

    expect(error).toMatchSnapshot()
  })
})
