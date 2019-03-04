// // @flow

import { Parser } from './Parser'
import { flags as Flags } from './Flags/index'

test('parses args and flags', async () => {
  const p = new Parser({
    args: [{ name: 'myarg' }, { name: 'myarg2' }],
    flags: { myflag: Flags.string() },
  })
  const { argv, flags } = await p.parse({
    argv: ['foo', '--myflag', 'bar', 'baz'],
  })
  expect(argv![0]).toEqual('foo')
  expect(argv![1]).toEqual('baz')
  expect(flags!.myflag).toEqual('bar')
})

describe('flags', () => {
  test('parses flags', async () => {
    const p = new Parser({
      flags: { myflag: Flags.boolean(), myflag2: Flags.boolean() },
    })
    const { flags } = await p.parse({ argv: ['--myflag', '--myflag2'] })
    expect(flags!.myflag).toBeTruthy()
    expect(flags!.myflag2).toBeTruthy()
  })

  test('parses short flags', async () => {
    const p = new Parser({
      flags: {
        myflag: Flags.boolean({ char: 'm' }),
        force: Flags.boolean({ char: 'f' }),
      },
    })
    const { flags } = await p.parse({ argv: ['-mf'] })
    expect(flags!.myflag).toBeTruthy()
    expect(flags!.force).toBeTruthy()
  })

  test('parses flag value with "=" to separate', async () => {
    const p = new Parser({
      flags: {
        myflag: Flags.string({ char: 'm' }),
      },
    })
    const { flags } = await p.parse({ argv: ['--myflag=foo'] })
    expect(flags).toEqual({ myflag: 'foo' })
  })

  test('parses flag value with "=" in value', async () => {
    const p = new Parser({
      flags: {
        myflag: Flags.string({ char: 'm' }),
      },
    })
    const { flags } = await p.parse({ argv: ['--myflag', '=foo'] })
    expect(flags).toEqual({ myflag: '=foo' })
  })

  test('parses short flag value with "="', async () => {
    const p = new Parser({
      flags: {
        myflag: Flags.string({ char: 'm' }),
      },
    })
    const { flags } = await p.parse({ argv: ['-m=foo'] })
    expect(flags).toEqual({ myflag: 'foo' })
  })

  test('requires required flag', async () => {
    const p = new Parser({
      flags: {
        myflag: Flags.string({ required: true }),
      },
    })
    expect.assertions(1)
    try {
      await p.parse()
    } catch (err) {
      expect(err.message).toEqual('Missing required flag --myflag')
    }
  })

  test('requires nonoptional flag', async () => {
    const p = new Parser({
      flags: {
        myflag: Flags.string({ required: true }),
      },
    })
    expect.assertions(1)
    try {
      await p.parse()
    } catch (err) {
      expect(err.message).toEqual('Missing required flag --myflag')
    }
  })

  test('removes flags from argv', async () => {
    const p = new Parser({
      args: [{ name: 'myarg' }],
      flags: { myflag: Flags.string() },
    })
    const { flags, argv } = await p.parse({ argv: ['--myflag', 'bar', 'foo'] })
    expect(flags).toEqual({ myflag: 'bar' })
    expect(argv).toEqual(['foo'])
  })
})

describe('args', () => {
  test('requires no args by default', async () => {
    expect.assertions(0)
    const p = new Parser({ args: [{ name: 'myarg' }, { name: 'myarg2' }] })
    try {
      await p.parse()
    } catch (err) {
      expect(err.message).toEqual('Missing required argument myarg')
    }
  })

  test('parses args', async () => {
    const p = new Parser({ args: [{ name: 'myarg' }, { name: 'myarg2' }] })
    const { argv } = await p.parse({ argv: ['foo', 'bar'] })
    expect(argv).toEqual(['foo', 'bar'])
  })

  test('skips optional args', async () => {
    const p = new Parser({
      args: [
        { name: 'myarg', required: false },
        { name: 'myarg2', required: false },
      ],
    })
    const { argv } = await p.parse({ argv: ['foo'] })
    expect(argv).toEqual(['foo'])
  })

  test('parses something looking like a flag as an arg', async () => {
    const p = new Parser({ args: [{ name: 'myarg' }] })
    const { argv } = await p.parse({ argv: ['--foo'] })
    expect(argv).toEqual(['--foo'])
  })
})

describe('variableArgs', () => {
  test('skips flag parsing after "--"', async () => {
    const p = new Parser({
      variableArgs: true,
      flags: { myflag: Flags.boolean() },
      args: [{ name: 'argOne' }],
    })
    const { argv, args } = await p.parse({
      argv: ['foo', 'bar', '--', '--myflag'],
    })
    expect(argv).toEqual(['foo', 'bar', '--myflag'])
    expect(args).toEqual({ argOne: 'foo' })
  })

  test('does not repeat arguments', async () => {
    const p = new Parser({
      variableArgs: true,
    })
    const { argv } = await p.parse({ argv: ['foo', '--myflag=foo bar'] })
    expect(argv).toEqual(['foo', '--myflag=foo bar'])
  })
})
