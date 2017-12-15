import { Importer } from './Importer'
import { Client, Config, Environment, Output } from 'graphcool-cli-engine'
import * as fs from 'fs'

async function load() {
  const types = fs.readFileSync(
    __dirname + '/fixtures/basic/types.graphql',
    'utf-8',
  )

  const config = new Config()
  const out = new Output(config)
  const env = new Environment(out, config)
  await env.load({})
  const client = new Client(config, env, out)
  const importer = new Importer(
    __dirname + '/fixtures/basic/.import',
    types,
    client,
    out,
    config,
  )

  importer.upload('cjb53jeus0bcv0139bq2liqvu')
}

load()
