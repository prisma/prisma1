import { EndpointDialog } from './EndpointDialog'
import { Output, Config, Client } from 'prisma-cli-engine'
import { Environment } from 'prisma-yml'
import { getTmpDir } from '../test/getTmpDir'
import { normalizeDockerCompose } from '../commands/init/index.test'

function makeDialog() {
  const config = new Config()
  const output = new Output(config)
  const env = new Environment(getTmpDir(), output)
  const client = new Client(config, env, output)
  const dialog = new EndpointDialog(output, client, env, config)

  return dialog
}

describe('endpoint dialog', () => {
  const dialog = makeDialog()
  test('local', async () => {
    const input = {
      choice: 'local',
      loggedIn: false,
      folderName: 'some-folder',
      localClusterRunning: false,
    }
    const result = await dialog.handleChoice(input)
    expect({ input, result }).toMatchSnapshot()
  })
  test('local running', async () => {
    const input = {
      choice: 'local',
      loggedIn: false,
      folderName: 'some-folder',
      localClusterRunning: true,
    }
    const result = await dialog.handleChoice(input)
    expect({ input, result }).toMatchSnapshot()
  })
})

export function normalizeResult({ dockerComposeYml, ...result }) {
  return {
    ...result,
    dockerComposeYml: normalizeDockerCompose(dockerComposeYml),
  }
}
