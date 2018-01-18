import { Importer } from './Importer'
import { Client, Config, Output } from 'prisma-cli-engine'
import { Environment } from 'prisma-yml'
import * as fs from 'fs'

async function load() {
  const types = fs.readFileSync(
    __dirname + '/fixtures/basic/types.graphql',
    'utf-8',
  )

  const config = new Config()
  const out = new Output(config)

  const env = new Environment(config.globalConfigPath, out)
  await env.load({})
  const client = new Client(config, env, out)
  const importer = new Importer(
    __dirname + '/fixtures/basic/.import',
    types,
    client,
    out,
    config,
  )

  importer.upload('service-name', 'dev')
}

load()
