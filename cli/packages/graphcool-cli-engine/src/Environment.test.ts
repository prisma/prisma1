import {Environment} from './Environment'
import {Output} from './Output'
import {Config} from './Config'

// lets write some tests

function makeEnvironment() {
  const config = new Config()
  const out = new Output(config)
  return new Environment(out, config)
}

describe('environment', () => {
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

    await env.loadRCs(localFile, null)
    expect(env.rc).toMatchSnapshot()
  })
  test('should resolve a target alias refering before', async () => {
    const env = makeEnvironment()
    const localFile = `
platformToken: 'secret-token'
targets:
  dev: local/asdasd123
  test-alias: dev
clusters:
  local:
    host: http://localhost:60000
    token: asdf
`

    await env.loadRCs(localFile, null)
    expect(env.rc).toMatchSnapshot()
  })
  test('should resolve a target alias refering after', async () => {
    const env = makeEnvironment()
    const localFile = `
platformToken: 'secret-token'
targets:
  test-alias: dev
  dev: local/asdasd123
clusters:
  local:
    host: http://localhost:60000
    token: asdf
`

    await env.loadRCs(localFile, null)
    expect(env.rc).toMatchSnapshot()
  })
//
//   test('throws when cluster does not exist', async () => {
//     const env = makeEnvironment()
//     const localFile = `
// platformToken: 'secret-token'
// targets:
//   dev: local/asdasd123
// `
//     env.loadRCs(localFile, null)
//     await expect(env.out.stderr.output).toMatch(/Could not find cluster local defined for target dev in/)
//   })

  test('should access targets from the global RC', async () => {
    const env = makeEnvironment()
    const localFile = `
platformToken: 'secret-token'
targets:
  test-alias: dev
  dev: local/asdasd123
`
    const globalFile = `
clusters:
  local:
    host: http://localhost:60000
    token: asdf
    `

    await env.loadRCs(localFile, globalFile)
    expect(env.rc).toMatchSnapshot()
  })
  test('should resolve target from the global RC', async () => {
    const env = makeEnvironment()
    const localFile = `
platformToken: 'secret-token'
targets:
  test-alias: dev
`
    const globalFile = `
targets:
  dev: local/asdasd123
clusters:
  local:
    host: http://localhost:60000
    token: asdf
    `

    await env.loadRCs(localFile, globalFile)
    expect(env.rc).toMatchSnapshot()
  })
  test('should override global target', async () => {
    const env = makeEnvironment()
    const localFile = `
platformToken: 'secret-token'
targets:
  dev: shared-eu-west-1/asdfasdf
`
    const globalFile = `
targets:
  dev: local/asdasd123
clusters:
  local:
    host: http://localhost:60000
    token: asdf
    `

    await env.loadRCs(localFile, globalFile)
    expect(env.rc).toMatchSnapshot()
  })
})
