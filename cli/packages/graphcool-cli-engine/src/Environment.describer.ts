import { Environment } from './Environment'
import { Output } from './Output'
import { Config } from './Config'

// lets write some tests

function makeEnvironment() {
  const config = new Config()
  const out = new Output(config)
  return new Environment(out, config)
}

const env = makeEnvironment()
const localFile = `
platformToken: 'secret-token'
clusters:
  local:
    host: http://localhost:60000
    token: asdf
`

env.load({})
console.log(env.globalRC)
