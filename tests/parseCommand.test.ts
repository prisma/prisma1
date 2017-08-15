import test from 'ava'
import { parseCommand } from '../src/utils/parseCommand'
import { getArgv } from './helpers/test_argv'
const {version} = require('../../package.json')
import { testEnvironment } from './helpers/test_environment'
import { Command } from '../src/types'

const env = testEnvironment({})

function getCmd(input: string) {
  return parseCommand(getArgv(input), version, env)
}

test('init without args', async t => {
  const instruction = await getCmd('init')
  t.deepEqual(instruction, {
    command: 'interactiveInit' as Command,
    props: {
      alias: undefined,
      name: undefined,
      outputPath: undefined,
      region: undefined,
    }
  })
})

test('init with all args', async t => {
  const instruction = await getCmd('init -a alias -n "Project Name" -o "output.graphcool" -r "us-west-2"')
  t.deepEqual(instruction, {
    command: 'interactiveInit' as Command,
    props: {
      alias: 'alias',
      name: 'Project Name',
      outputPath: 'output.graphcool',
      region: 'us-west-2',
    }
  })
})

test('push', async t => {
  const instruction = await getCmd('push')
  t.deepEqual(instruction, {
    command: 'push' as Command,
    props: {force: false, projectFile: undefined}
  })
})

test('delete', async t => {
  const instruction = await getCmd('delete')
  t.deepEqual(instruction, {
    command: 'delete' as Command,
    props: {sourceProjectId: undefined}
  })
})

test('pull', async t => {
  const instruction = await getCmd('pull')
  t.deepEqual(instruction, {
    command: 'pull' as Command,
    props: {
      sourceProjectId: undefined,
      projectFile: undefined,
      outputPath: undefined,
      force: false
    }
  })
})

test('export', async t => {
  const instruction = await getCmd('export')
  t.deepEqual(instruction, {
    command: 'export' as Command,
    props: { projectFile: undefined }
  })
})

test('status', async t => {
  const instruction = await getCmd('status')
  t.deepEqual(instruction, {
    command: 'status' as Command,
    props: { projectFile: undefined }
  })
})

test('endpoints', async t => {
  const instruction = await getCmd('endpoints')
  t.deepEqual(instruction, {
    command: 'endpoints' as Command,
    props: { projectFile: undefined }
  })
})

test('console', async t => {
  const instruction = await getCmd('console')
  t.deepEqual(instruction, {
    command: 'console' as Command,
    props: { projectFile: undefined }
  })
})

test('playground', async t => {
  const instruction = await getCmd('playground')
  t.deepEqual(instruction, {
    command: 'playground' as Command,
    props: { projectFile: undefined }
  })
})

test('projects', async t => {
  const instruction = await getCmd('projects')
  t.deepEqual(instruction, {
    command: 'projects' as Command,
  })
})

test('auth', async t => {
  const instruction = await getCmd('auth')
  t.deepEqual(instruction, {
    command: 'auth' as Command,
    props: {token: undefined}
  })
})

test('quickstart', async t => {
  const instruction = await getCmd('quickstart')
  t.deepEqual(instruction, {
    command: 'quickstart' as Command,
  })
})

test('help', async t => {
  const instruction = await getCmd('help')
  t.deepEqual(instruction, {
    command: 'help' as Command,
  })
})

test('version', async t => {
  const instruction = await getCmd('version')
  t.deepEqual(instruction, {
    command: 'version' as Command,
  })
})
