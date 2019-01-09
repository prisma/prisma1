import { replaceYamlValue, migrateToEndpoint } from './yamlComment'

describe('replaceYamlValue', () => {
  test('when document is clean', () => {
    const input = `\
endpoint: https://eu1.prisma.sh/public-asdf/my-service/dev
datamodel: datamodel.prisma`

    const output = replaceYamlValue(input, 'endpoint', 'http://localhost:4466')

    expect({ input, output }).toMatchSnapshot()
  })

  test('when comments already exist', () => {
    const input = `\
#anothercomment: asdasd
endpoint: https://eu1.prisma.sh/public-asdf/my-service/dev

#endpoint: asdasd
datamodel: datamodel.prisma`

    const output = replaceYamlValue(input, 'endpoint', 'http://localhost:4466')

    expect({ input, output }).toMatchSnapshot()
  })

  test('when key does not yet exist', () => {
    const input = `\
datamodel: datamodel.prisma`

    const output = replaceYamlValue(input, 'endpoint', 'http://localhost:4466')

    expect({ input, output }).toMatchSnapshot()
  })
})

describe('migrateToEndpoint', () => {
  test('ignore when endpoint present', () => {
    const input = `\
endpoint: https://eu1.prisma.sh/public-asdf/my-service/dev
datamodel: datamodel.prisma`

    const output = migrateToEndpoint(input, '')

    expect({ input, output }).toMatchSnapshot()
  })

  test('work in simple case', () => {
    const input = `\
service: my-service
stage: dev
cluster: public-asdf/prisma-eu1
datamodel: datamodel.prisma`

    const output = migrateToEndpoint(
      input,
      'https://eu1.prisma.sh/public-asdf/my-service/dev',
    )

    expect({ input, output }).toMatchSnapshot()
  })

  test('work with different order and respect comments', () => {
    const input = `\
# don't delete me
stage: dev
cluster: public-asdf/prisma-eu1

service: my-service



datamodel: datamodel.prisma`

    const output = migrateToEndpoint(
      input,
      'https://eu1.prisma.sh/public-asdf/my-service/dev',
    )

    expect({ input, output }).toMatchSnapshot()
  })
})
