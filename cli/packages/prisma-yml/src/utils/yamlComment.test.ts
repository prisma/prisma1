import { replaceYamlValue } from './yamlComment'

describe('replaceYamlValue', () => {
  test('when document is clean', () => {
    const input = `\
    endpoint: https://eu1.prisma.sh/public-asdf/my-service/dev
    datamodel: datamodel.graphql`

    expect(
      replaceYamlValue(input, 'endpoint', 'http://localhost:4466'),
    ).toMatchSnapshot()
  })

  test('when comments already exist', () => {
    const input = `\
    #anothercomment: asdasd
    endpoint: https://eu1.prisma.sh/public-asdf/my-service/dev

    #endpoint: asdasd
    datamodel: datamodel.graphql`

    expect(
      replaceYamlValue(input, 'endpoint', 'http://localhost:4466'),
    ).toMatchSnapshot()
  })

  test('when key does not yet exist', () => {
    const input = `\
    datamodel: datamodel.graphql`

    expect(
      replaceYamlValue(input, 'endpoint', 'http://localhost:4466'),
    ).toMatchSnapshot()
  })
})
