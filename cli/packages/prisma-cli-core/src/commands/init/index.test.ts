import * as nock from 'nock'
import { Config } from 'prisma-cli-engine'
import Init from './'

afterAll(() => {
  nock.cleanAll()
})

describe.skip('init', () => {
  test('test project', async () => {
    const result = await Init
      .mock
      //'-t',
      //'blank',
      ()
    expect(result.out.stdout.output).toContain('Written files:')
  })
})
