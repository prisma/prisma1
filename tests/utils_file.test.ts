import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import { Config } from '../src/utils/config'
import TestOut from '../src/system/TestOut'
import { graphcoolProjectFileName } from '../src/utils/constants'
import { TestSystemEnvironment } from '../src/types'
import { mockProjectFile1, mockProjectFileWithUppercaseAlias1 } from './fixtures/mock_data'
import { readProjectIdFromProjectFile } from '../src/utils/file'

/*
 Tests:
 - readProjectIdFromProjectFile with a file containing a project id
 - readProjectIdFromProjectFile with a file containing a project alias
 - readProjectIdFromProjectFile with a file missing a project id
 */

test.afterEach(() => {
  new TestOut().write('\n')
})

test('readProjectIdFromProjectFile with a file containing a project id', async t => {
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, mockProjectFile1)

  const project_id = readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName)

  t.is(project_id, 'abcdefghijklmn')
})

test('readProjectIdFromProjectFile with a file containing a project alias', async t => {
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, mockProjectFileWithUppercaseAlias1)

  const project_id = readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName)

  t.is(project_id, 'Example')
})

test('readProjectIdFromProjectFile with a file missing a project id', async t => {
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, '')

  const project_id = readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName)

  t.is(project_id, undefined)
})

function testEnvironment(storage: any): TestSystemEnvironment {
  const resolver = new TestResolver(storage)
  const config = new Config(resolver)

  return {
    resolver: resolver,
    out: new TestOut(),
    config: config
  }
}
