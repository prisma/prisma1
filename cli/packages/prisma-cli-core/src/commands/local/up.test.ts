import Up from './up'

jest.mock('./Docker')
const Docker = require('./Docker').default
Docker.mockImplementation(() => {
  return {
    up: () => {
      return Promise.resolve({
        envVars: {
          MASTER_TOKEN: 'token',
          PORT: '4466',
        },
        hostName: 'localhost',
      })
    },
  }
})

describe.skip('up', () => {
  test('in empty dir', async () => {
    const result = await Up.mock()
    expect(result.out.stdout.output).toContain('Success! Added local cluster')
  })
})
