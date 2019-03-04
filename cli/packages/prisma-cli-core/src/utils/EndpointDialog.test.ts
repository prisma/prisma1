import { EndpointDialog } from './EndpointDialog'
import { Output, Config, Client } from 'prisma-cli-engine'
import { Environment, PrismaDefinitionClass } from 'prisma-yml'
import { getTmpDir } from '../test/getTmpDir'
import { normalizeDockerCompose } from '../commands/init/init.test'

function makeDialog() {
  const config = new Config()
  const output = new Output(config)
  const env = new Environment(getTmpDir(), output)
  const client = new Client(config, env, output)
  const definition = new PrismaDefinitionClass(env)

  const dialog = new EndpointDialog({
    out: output,
    client,
    env,
    config,
    definition,
    shouldAskForGenerator: false,
  })

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
      existingData: false,
    }
    const result = await dialog.handleChoice(input)
    expect({ input, result: normalizeResult(result) }).toMatchSnapshot()
  })
  test('local running', async () => {
    const input = {
      choice: 'local',
      loggedIn: false,
      folderName: 'some-folder',
      localClusterRunning: true,
      existingData: false,
    }
    const result = await dialog.handleChoice(input)
    expect({ input, result: normalizeResult(result) }).toMatchSnapshot()
  })
})

export function normalizeResult({ dockerComposeYml, ...result }) {
  return {
    ...result,
    dockerComposeYml: normalizeDockerCompose(dockerComposeYml),
  }
}
