import {Environment} from './Environment'
import {Output} from './Output'
import {Config} from './Config'
import {Client} from './Client/Client'

// lets write some tests

function makeEnvironment() {
  const config = new Config()
  const out = new Output(config)
  const client = new Client(config)
  return new Environment(out, config, client)
}

describe('config', () => {
  test('should work without global RC', async () => {
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

    debugger
    env.loadRCs(localFile, null)
    expect(env.localRC).toMatchSnapshot()
  })
  // test('should access targets from the global RC', async () => {
  // })
  // test('should override targets from the global RC', async () => {
  // })
})
