import { GraphcoolDefinitionClass } from './GraphcoolDefinition'
import { Config } from '../Config'
import { Output } from '../index'
import * as fs from 'fs-extra'
import * as path from 'path'
import { Environment } from '../Environment'

function makeDefinition() {
  const config = new Config()
  const out = new Output(config)
  return new GraphcoolDefinitionClass(out, config)
}

describe('graphcool definition', () => {
  test('load basic yml', async () => {
    const yml = `\
service: jj

datamodel:
- datamodel.graphql

schema: schemas/database.graphql
stages: 
  default: dev
  dev: shared-eu-west-1
    `
    const datamodel = `
type User @model {
  id: ID! @isUnique
  name: String!
  lol: Int
  what: String
}
`
    const config = new Config()
    const out = new Output(config)
    const definition = new GraphcoolDefinitionClass(out, config)
    const env = new Environment(out, config)
    // config.definitionPath =

    fs.writeFileSync(
      path.join(config.definitionDir, 'datamodel.graphql'),
      datamodel,
    )
    const definitionPath = path.join(config.definitionDir, 'graphcool.yml')
    fs.writeFileSync(definitionPath, yml)
    await env.load({})

    config.definitionPath = definitionPath

    await definition.load(env, {})

    expect(definition.definition).toMatchSnapshot()
  })
})
