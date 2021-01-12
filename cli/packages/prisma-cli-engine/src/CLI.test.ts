import { CLI } from './CLI'

jest.unmock('fs-extra')

jasmine.DEFAULT_TIMEOUT_INTERVAL = 60000

async function run(...argv: string[]) {
  const cli = new CLI({
    config: { argv: ['prisma1'].concat(argv), mock: true },
  })
  try {
    await cli.run()
    return cli
  } catch (err) {
    if (err.code !== 0) {
      throw err
    }
    return cli
  }
}

// test('runs the version command', async () => {
//   expect.assertions(1)
//   const cli = new CLI({config: {argv: ['prisma', 'version'], mock: true}})
//   try {
//     await cli.run()
//   } catch (err) {
//     expect(err.code).toBe(0)
//   }
// })
//
// test('errors with invalid arguments', async () => {
//   expect.assertions(1)
//   const cli = new CLI({config: {argv: ['prisma', 'version', '--invalid-flag'], mock: true}})
//   try {
//     await cli.run()
//   } catch (err) {
//     expect(err.message).toContain('Unexpected argument --invalid-flag')
//   }
// })
//
// test('errors when command not found', async () => {
//   expect.assertions(1)
//   const cli = new CLI({config: {argv: ['prisma', 'foobar12345'], mock: true}})
//   try {
//     await cli.run()
//   } catch (err) {
//     if (!err.code) {
//       throw err
//     }
//     expect(err.code).toEqual(127)
//   }
// })

describe('edge cases', () => {
  test('shows help for `help` command itself', async () => {
    const cli = await run('help')
    expect(cli.cmd.out.stdout.output).toMatch(/Usage: prisma1 COMMAND/)
  })
})

describe('cli help', () => {
  describe('global help', () => {
    const globalHelpOutput = /^Usage: \S+ COMMAND/m

    test('shows help when no arguments given', async () => {
      const cli = await run()
      expect(cli.cmd.out.stdout.output).toMatch(globalHelpOutput)
    })

    test('shows help for `help` command and no additonal arguments', async () => {
      const cli = await run('help')
      expect(cli.cmd.out.stdout.output).toMatch(globalHelpOutput)
    })

    test('shows help for `--help` or `-h` flag and no additonal arguments', async () => {
      const cli = await run('--help')
      const clid = await run('-h')
      expect(cli.cmd.out.stdout.output).toMatch(globalHelpOutput)
      expect(clid.cmd.out.stdout.output).toMatch(globalHelpOutput)
    })
  })
})

describe('cli version', () => {
  test('-v', async () => {
    const cli = await run('-v')
    expect(cli.cmd.out.stdout.output).toContain('/1.1')
  })
  test('--version', async () => {
    const cli = await run('--version')
    expect(cli.cmd.out.stdout.output).toContain('/1.1')
  })
  test('version', async () => {
    const cli = await run('version')
    expect(cli.cmd.out.stdout.output).toContain('/1.1')
  })
})
