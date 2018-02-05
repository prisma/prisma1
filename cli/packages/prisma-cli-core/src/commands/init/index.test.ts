import * as nock from 'nock'
import * as fs from 'fs-extra'
import * as path from 'path'
import { Config } from 'prisma-cli-engine'
import { getTmpDir } from '../../test/getTmpDir'
import Init from './'

afterAll(() => {
  nock.cleanAll()
})

describe('init', () => {
  test('test project', async () => {

    // I am not really sure what I need to do with this?
    const tmpDir = getTmpDir()

    const testFolder = './'

    // This doesn't see to be the correct test.
    fs.readdir(tmpDir, (err, files) => {
      files.map(file => expect(file).toMatchSnapshot())
    })

    // console.log(tmpDir)
    // const result = await Init
    //   .mock
    //   //'-t',
    //   //'blank',
    //   ()
    // expect(result.out.stdout.output).toContain('Written files:')
    // expect(true).toBe(true)
  })
})
