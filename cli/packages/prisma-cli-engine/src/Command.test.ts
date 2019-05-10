/* tslint:disable */
import 'source-map-support/register'

import { Command as Base } from './Command'
import { flags as Flags } from './Flags'
import nock from 'nock'
import { Config } from './Config'

class Command extends Base {
  static topic = 'foo'
  static command = 'bar'
  static flags = { myflag: Flags.boolean() }
  static args = [{ name: 'myarg', required: false }]
}

test('gets the version tokens correctly', async () => {
  const cmd = await Command.mock()
  expect(cmd.getVersionTokens('1.30.0')).toEqual({
    minorVersion: '1.30',
    stage: 'master'
  })
  expect(cmd.getVersionTokens('1.31.0-beta')).toEqual({
    minorVersion: '1.31',
    stage: 'beta'
  })
  expect(cmd.getVersionTokens('1.31.0-beta.5')).toEqual({
    minorVersion: '1.31',
    stage: 'beta'
  })
  expect(cmd.getVersionTokens('1.31.2-alpha')).toEqual({
    minorVersion: '1.31',
    stage: 'alpha'
  })
  expect(cmd.getVersionTokens('1.31.0-alpha.5')).toEqual({
    minorVersion: '1.31',
    stage: 'alpha'
  })
  expect(cmd.getVersionTokens('1.31-alpha.5')).toEqual({
    minorVersion: '1.31',
    stage: 'alpha'
  })
  expect(cmd.getVersionTokens('1.31.2-alpha.5')).toEqual({
    minorVersion: '1.31',
    stage: 'alpha'
  })
  expect(cmd.getVersionTokens('1.31-alpha-7')).toEqual({
    minorVersion: '1.31',
    stage: 'alpha'
  })
})

test('compares the versions correctly', async () => {
  const cmd = await Command.mock()
  expect(cmd.compareVersions('1.30.0', '1.29.0')).toBe(false)
  expect(cmd.compareVersions('1.30.2', '1.30.2')).toBe(true)
  expect(cmd.compareVersions('1.30.0', '1.30.2')).toBe(true)
  expect(cmd.compareVersions('1.30.2-alpha', '1.30.0')).toBe(false)
  expect(cmd.compareVersions('1.30.2-alpha', '1.30.0-beta')).toBe(false)
  expect(cmd.compareVersions('1.30.2-alpha', '1.30.5-alpha')).toBe(true)
  expect(cmd.compareVersions('1.30.2-alpha', '1.31.0-alpha')).toBe(false)
  expect(cmd.compareVersions('1.30.2-alpha.5', '1.31.0-alpha.7')).toBe(false)
  expect(cmd.compareVersions('1.31.2-alpha.5', '1.31-alpha.7')).toBe(true)
  expect(cmd.compareVersions('1.31.2-alpha.5', '1.31-alpha-7')).toBe(true)
})

test('shows the ID', () => {
  expect(Command.id).toEqual('foo:bar')
})

test('runs the command', async () => {
  const cmd = await Command.mock()
  expect(cmd.flags).toEqual({})
  expect(cmd.argv).toEqual([])
})

test('has stdout', async () => {
  class Command extends Base {
    static flags = {
      print: Flags.string(),
      bool: Flags.boolean(),
    }
    async run() {
      this.out.stdout.log(this.flags.print)
    }
  }

  const { stdout } = await Command.mock('--print=foo')
  expect(stdout).toEqual('foo\n')
})

test('has stderr', async () => {
  class Command extends Base {
    static flags = { print: Flags.string() }
    async run() {
      this.out.stderr.log(this.flags.print)
    }
  }

  const { stderr } = await Command.mock('--print=foo')
  expect(stderr).toEqual('foo\n')
})

test('parses args', async () => {
  const cmd = await Command.mock('one')
  expect(cmd.flags).toEqual({})
  expect(cmd.argv).toEqual(['one'])
  expect(cmd.args).toEqual({ myarg: 'one' })
})

test('has help', async () => {
  class Command extends Base {
    static topic = 'config'
    static command = 'get'
    static help = `this is

some multiline help
`
  }
  let config = new Config({ mock: true })
  expect(Command.buildHelp(config)).toMatchSnapshot('buildHelp')
  expect(Command.buildHelpLine(config)).toMatchSnapshot('buildHelpLine')
})
