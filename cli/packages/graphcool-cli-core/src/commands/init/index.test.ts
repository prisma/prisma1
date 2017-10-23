import * as nock from 'nock'
import { Config } from 'graphcool-cli-engine'
import Init from './'

afterAll(() => {
  nock.cleanAll()
})

describe('init', () => {
  test('test project', async () => {
    const result = await Init.mock(
      //'-t',
      //'blank',
    )
    expect(result.out.stdout.output).toContain('Written files:')
  })
})
