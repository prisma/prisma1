import { Cluster, Output } from '.'

describe('cluster endpoint generation', () => {
  test('local cluster', () => {
    const cluster = new Cluster(
      new Output(),
      'local',
      'http://localhost:4466',
      undefined,
      true,
    )
    expect(cluster.getApiEndpoint('default', 'default')).toMatchSnapshot()
    expect(cluster.getApiEndpoint('dev', 'default')).toMatchSnapshot()
    expect(cluster.getApiEndpoint('default', 'dev')).toMatchSnapshot()
    expect(
      cluster.getApiEndpoint('default', 'dev', 'ignore-me'),
    ).toMatchSnapshot()
  })
  test('private cluster', () => {
    const cluster = new Cluster(
      new Output(),
      'test01',
      'https://test01_workspace.prisma.sh',
      undefined,
      false,
      false,
      true,
    )
    expect(
      cluster.getApiEndpoint('default', 'default', 'workspace'),
    ).toMatchSnapshot()
    expect(
      cluster.getApiEndpoint('dev', 'default', 'workspace'),
    ).toMatchSnapshot()
    expect(
      cluster.getApiEndpoint('default', 'dev', 'workspace'),
    ).toMatchSnapshot()
  })
  test('sandbox cluster', () => {
    const cluster = new Cluster(
      new Output(),
      'prisma-eu1',
      'https://eu1.prisma.sh',
      undefined,
      false,
      true,
      false,
    )
    expect(
      cluster.getApiEndpoint('default', 'default', 'workspace'),
    ).toMatchSnapshot()
    expect(
      cluster.getApiEndpoint('dev', 'default', 'workspace'),
    ).toMatchSnapshot()
    expect(
      cluster.getApiEndpoint('default', 'dev', 'workspace'),
    ).toMatchSnapshot()
  })
})
