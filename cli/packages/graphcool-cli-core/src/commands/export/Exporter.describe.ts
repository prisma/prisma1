import { Exporter } from './Exporter'
import { Client, Config, Output } from 'graphcool-cli-engine'
import { Environment } from 'graphcool-yml'
import * as fs from 'fs'

async function load() {
  const config = new Config()
  const out = new Output(config)
  const env = new Environment(config.globalRCPath, out)
  await env.load({})
  const client = new Client(config, env, out)
  const exporter = new Exporter(__dirname + '/download', client, out, config)

  await exporter.download('service', 'dev')
}

load()
