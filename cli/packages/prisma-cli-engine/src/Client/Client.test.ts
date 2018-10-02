import { Client } from './Client'
import { Config } from '../Config'
import { Environment } from 'prisma-yml'
import { Output } from '../index'

import * as opn from 'opn'
import { GraphQLClient } from 'graphql-request'
jest.mock('graphql-request')
jest.mock('opn')

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
it('should log that url could not be open and keep listening for login', async () => {
  GraphQLClient.mockImplementation(() => {
    return {
      request: () => {
        return {
          requestCloudToken: {
            secret: 'my-secret'
          }
        }
      },
    }
  })
  opn.mockImplementation(() => {
    return new Promise((resolve, reject) => {
      reject(new Error('Exited with code XY'));
			return;
		});
  })
  const config = new Config()
  const env = new Environment(config.home)
  const output = new Output(config)
  const client = new Client(config, env, output)
  try {
    const res = await client.login()
  } catch(err) {
    // irrelevant for this test
  }
  expect(output.stdout.output).toMatchSnapshot()
})