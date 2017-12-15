import { Exporter } from './Exporter'
import { Client, Config, Environment, Output } from 'graphcool-cli-engine'
import * as fs from 'fs'

async function load() {
  const config = new Config()
  const out = new Output(config)
  const env = new Environment(out, config)
  await env.load({})
  const client = new Client(config, env, out)
  const exporter = new Exporter(__dirname + '/download', client, out, config)

  await exporter.download('cjb53jeus0bcv0139bq2liqvu')
}

load()
