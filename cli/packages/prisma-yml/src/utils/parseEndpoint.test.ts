import { parseEndpoint } from './parseEndpoint'

describe('parseEndpoint', () => {
  test('work for minimal url', () => {
    expect(parseEndpoint('http://localhost:4466')).toMatchSnapshot()
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
})
