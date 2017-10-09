import { Environment } from './Environment'
import { Output } from './Output'
import { Config } from './Config'
import { Client } from './Client/Client'

// lets write some tests

function makeEnvironment() {
  const config = new Config()
  const out = new Output(config)
  const client = new Client(config)
  return new Environment(out, config, client)
}

const env = makeEnvironment()
const localFile = `
platformToken: 'secret-token'
targets:
  dev: local/asdasd123
clusters:
  local:
    host: http://localhost:60000
    token: asdf
`

console.log('debugga')
env.loadRCs(localFile, null)
console.log(env.localRC)
console.log(env.globalRC)
debugger
