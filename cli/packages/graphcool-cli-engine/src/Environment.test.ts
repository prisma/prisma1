import { Environment } from './Environment'
import { Output } from './Output'
import { Config } from './Config'

// // lets write some tests

function makeEnvironment() {
  const config = new Config()
  const out = new Output(config)
  return new Environment(out, config)
}

describe('environment', () => {
  test('should work with one cluster', async () => {
    const env = makeEnvironment()
    const localFile = `
clusters:
  local:
    host: http://localhost:60000
    clusterSecret: asdf
`

    await env.parseGlobalRC(localFile)
    const cluster = env.clusterByName('local')!
    expect(cluster.token).toBe('asdf')
    expect(cluster.baseUrl).toBe('http://localhost:60000')
  })

  test('should work with multiple clusters', async () => {
    const env = makeEnvironment()
    const localFile = `
clusters:
  local:
    host: http://localhost:60000
    clusterSecret: asdf
  special:
    host: https://special-cluster.graph.cool
`

    await env.parseGlobalRC(localFile)
    const cluster = env.clusterByName('local')!
    expect(cluster.token).toBe('asdf')
    expect(cluster.baseUrl).toBe('http://localhost:60000')
    const specialCluster = env.clusterByName('special')!
    expect(specialCluster.token).toBe(undefined)
    expect(specialCluster.baseUrl).toBe('https://special-cluster.graph.cool')
  })
})
