import { parseEndpoint } from './parseEndpoint'

describe('parseEndpoint', () => {
  test('work for minimal url', () => {
    expect(parseEndpoint('http://localhost:4466')).toMatchSnapshot()
  })
  test('work for minimal docker url', () => {
    expect(parseEndpoint('http://prisma:4466')).toMatchSnapshot()
  })
  test('local behind a proxy', () => {
    // This snapshot will be incorrect for now as URL does not have enough 
    // information to detemine if something is truly local
    expect(parseEndpoint('http://local.dev')).toMatchSnapshot()
  })
  test('work for url with service', () => {
    expect(
      parseEndpoint('http://localhost:4466/service-name'),
    ).toMatchSnapshot()
  })
  test('work for url with service and stage', () => {
    expect(
      parseEndpoint('http://localhost:4466/service-name/stage'),
    ).toMatchSnapshot()
  })
  test('private url', () => {
    expect(
      parseEndpoint('https://test1_workspace.prisma.sh/tessst/dev'),
    ).toMatchSnapshot()
  })
  test('shared url', () => {
    expect(
      parseEndpoint('https://eu1.prisma.sh/workspace-name/tessst/dev'),
    ).toMatchSnapshot()
  })
  test('custom hosted url in internet', () => {
    expect(
      parseEndpoint('https://api-prisma.divyendusingh.com/zebra-4069/dev'),
    ).toMatchSnapshot()
  })
  test('custom hosted url as ip in internet', () => {
    expect(
      parseEndpoint('http://13.228.39.83:4466'),
    ).toMatchSnapshot()
  })
  test('url on a subdomain', () => {
    expect(
      parseEndpoint('https://db.cloud.prisma.sh/test-token/test'),
    ).toMatchSnapshot()
  })
})
