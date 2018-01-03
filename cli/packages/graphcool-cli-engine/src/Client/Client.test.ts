import { Client } from './Client'
import { Config } from '../Config'
import { Environment } from '../../../graphcool-yml/dist/Environment'
import { Output } from '../index'

test('throws when no cluster provided', async () => {
  const config = new Config()
  const env = new Environment(config.home)
  const output = new Output(config)
  const client = new Client(config, env, output)
  let error
  try {
    await client.listProjects()
  } catch (e) {
    error = e
  }

  expect(error).toMatchSnapshot()
})
