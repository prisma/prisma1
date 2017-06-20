import test from 'ava'
import TestResolver from './helpers/test_resolver'
import TestOut from './helpers/test_out'
import { Config } from '../src/utils/config'
import { graphcoolConfigFilePath } from '../src/utils/constants'
const configFileContent = '{"token":"1234"}'

/*
 Tests:
 - load noops when config file does not exist
 - load obtains configs from file
 - set overrides specified configs
 - unset removes specified config
 - get returns config value
 - save persists configs to file
 */

test('load noops when config file does not exist', async t => {
  const resolver = new TestResolver({})
  var config = new Config(resolver)
  t.notThrows(() => { config.load() })
})

test('load obtains configs from file', async t => {
  const resolver = new TestResolver({})
  resolver.write(graphcoolConfigFilePath, configFileContent)

  var config = new Config(resolver)
  config.load()

  t.is(config.configs.token, '1234')
})

test('set overrides specified configs', async t => {
  const resolver = new TestResolver({})

  var config = new Config(resolver)
  config.set({ token: '1234' })
  t.is(config.configs.token, '1234')
})

test('unset removes specified config', async t => {
  const resolver = new TestResolver({})
  resolver.write(graphcoolConfigFilePath, configFileContent)

  var config = new Config(resolver)
  config.unset('token')
  t.is(config.configs.token, undefined)
})

test('get returns config value', async t => {
  const resolver = new TestResolver({})
  resolver.write(graphcoolConfigFilePath, configFileContent)

  var config = new Config(resolver)
  config.load()

  t.is(config.get('token'), '1234')
})

test('save persists configs to file', async t => {
  const resolver = new TestResolver({})
  resolver.write(graphcoolConfigFilePath, configFileContent)

  var config = new Config(resolver)
  config.load()

  config.set({ token: '4567' })
  config.save()

  const expectedConfig = `\
{
  "token": "4567"
}\
`
  t.is(resolver.read(graphcoolConfigFilePath), expectedConfig)
})
