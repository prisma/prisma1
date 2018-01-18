import { Exporter } from './Exporter'
import { Client, Config, Output } from 'prisma-cli-engine'
import { Environment } from 'prisma-yml'
import * as fs from 'fs'

async function load() {
  const config = new Config()
  const out = new Output(config)
  const env = new Environment(config.globalConfigPath, out)
  await env.load({})
  const client = new Client(config, env, out)
  const exporter = new Exporter(__dirname + '/download', client, out, config)

  await exporter.download('service', 'dev')
}

load()
