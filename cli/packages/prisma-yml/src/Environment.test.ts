import { Environment } from './Environment'
import { getTmpDir } from './test/getTmpDir'
import * as path from 'path'
import * as fs from 'fs-extra'
import { Cluster } from './Cluster'
import { Output } from './Output'

export function makeEnv(rc?: string) {
  const tmpDir = getTmpDir()
  return new Environment(tmpDir)
}

const out = new Output()

describe('Environment', () => {
  test('non-existent global prisma rc', async () => {
    const env = makeEnv()
    await env.load({})
    expect(env.clusters).toMatchSnapshot()
  })
  test('persists .prisma correctly', async () => {
    const env = makeEnv()
    await env.load({})
    const cluster = new Cluster(out, 'cluster', `http://localhost:60000`, '')
    env.addCluster(cluster)
    env.saveGlobalRC()
    expect(fs.readFileSync(env.rcPath, 'utf-8')).toMatchSnapshot()
    expect(env.clusters).toMatchSnapshot()
  })
  test('empty global prisma rc', async () => {
    const env = makeEnv('')
    await env.load({})
    expect(env.clusters).toMatchSnapshot()
  })
  test('sets the platform token correctly', async () => {
    const env = makeEnv(`platformToken: asdf`)
    await env.load({})
    expect(env.clusters).toMatchSnapshot()
  })
  test('interpolates env vars', async () => {
    process.env.SPECIAL_TEST_ENV_VAR = 'this-is-so-special'
    const env = makeEnv(`platformToken: \${env:SPECIAL_TEST_ENV_VAR}`)
    await env.load({})
    expect(env.clusters).toMatchSnapshot()
  })
  test('loads multiple cluster definitions correctly + gives cluster by name', async () => {
    const rc = `clusters:
    local:
      host: 'http://localhost:60000'
    remote:
      host: 'https://remote.graph.cool'
      clusterSecret: 'here-is-a-token'
  `
    const env = makeEnv(rc)
    await env.load({})
    expect(env.clusters).toMatchSnapshot()

    const cluster = env.clusterByName('remote')
    expect(cluster).toMatchSnapshot()
  })
})
