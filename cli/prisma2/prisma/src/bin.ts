#!/usr/bin/env ts-node

/**
 * Dependencies
 */
import { isError, HelpError, Env } from '@prisma/cli'
import { LiftCommand, LiftSave, LiftUp, LiftDown, LiftWatch, Converter } from '@prisma/lift'
import { CLI } from './CLI'
import { PhotonGenerate } from '@prisma/photon'
import { Introspect } from '@prisma/introspection'
import { Version } from './Version'
import { getCompiledGenerators } from './getCompiledGenerators'
import { Generate } from './Generate'

/**
 * Main function
 */
async function main(): Promise<number> {
  // load the environment
  const env = await Env.load(process.env, process.cwd())
  if (isError(env)) {
    console.error(env)
    return 1
  }
  const compiledGenerators = await getCompiledGenerators(env.cwd)
  // create a new CLI with our subcommands
  const cli = CLI.new({
    lift: LiftCommand.new(
      {
        save: LiftSave.new(env),
        up: LiftUp.new(env),
        down: LiftDown.new(env),
      },
      env,
    ),
    introspect: Introspect.new(env),
    convert: Converter.new(env),
    dev: LiftWatch.new(env, compiledGenerators),
    generate: Generate.new(env, compiledGenerators),
    version: Version.new(),
  })
  // parse the arguments
  var result = await cli.parse(process.argv.slice(2))
  if (result instanceof HelpError) {
    console.error(result.message)
    return 1
  } else if (isError(result)) {
    console.error(result)
    return 1
  }
  console.log(result)

  return 0
}

process.on('SIGINT', () => {
  process.exit(0) // now the "exit" event will fire
})

/**
 * Run our program
 */
main()
  .then(code => {
    if (code !== 0) {
      process.exit(code)
    }
  })
  .catch(err => {
    console.error(err)
    process.exit(1)
  })
