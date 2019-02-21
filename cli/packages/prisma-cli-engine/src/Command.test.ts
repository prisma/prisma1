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
