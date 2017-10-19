import { FunctionDefinition } from 'graphcool-json-schema'
import {
  GraphcoolModule,
  Config,
  Environment,
  Output,
} from 'graphcool-cli-engine'
import * as path from 'path'
import BufferedConsole from './BufferedConsole'
import getConsoleOutput from './getConsoleOutput'
import chalk from 'chalk'
import {register} from 'ts-node'
import { baseCompilerOptions } from './commands/deploy/Bundler/TypescriptBuilder'

export class LocalInvoker {
  fnName: string
  fnDefinition: FunctionDefinition
  module: GraphcoolModule
  config: Config
  env: Environment
  out: Output
  constructor(
    config: Config,
    env: Environment,
    out: Output,
    module: GraphcoolModule,
    fnName: string,
    fnDefinition: FunctionDefinition,
  ) {
    this.config = config
    this.env = env
    this.out = out
    this.module = module
    this.fnName = fnName
    this.fnDefinition = fnDefinition
  }

  public async invoke(input: any): Promise<any> {
    this.injectEnvironment()

    register({
      compilerOptions: {
        ...baseCompilerOptions,
        target: 'es5',
        moduleResolution: 'node',
        lib: ['es2017', 'dom']
      }
    })

    const src = typeof this.fnDefinition.handler.code === 'string' ? this.fnDefinition.handler.code : this.fnDefinition.handler.code!.src
    const functionPath = path.join(
      this.module.baseDir!,
      src,
    )

    let fnPointer = require(functionPath)
    fnPointer = fnPointer.default || fnPointer

    const localConsole = new BufferedConsole()

    const logTmp = console.log
    const errorTmp = console.error
    const warnTmp = console.warn
    const infoTmp = console.info

    console.log = localConsole.log
    console.error = localConsole.error
    console.warn = localConsole.warn
    console.info = localConsole.info

    let result
    try {
      const resultPointer = fnPointer(input)

      // TODO: Buffer console output like jest does
      // https://github.com/facebook/jest/blob/6c1016a56dc2d7ba70b428aab6eac2429f8aec7e/packages/jest-cli/src/runTest.js
      result =
        fnPointer.constructor.name === 'Promise'
          ? await resultPointer
          : resultPointer
    } catch (error) {
      console.error(error)
      this.out.exit(1)
    }

    console.log = logTmp
    console.error = errorTmp
    console.warn = warnTmp
    console.info = infoTmp

    const buffer = localConsole.getBuffer()
    if (buffer.length > 0) {
      this.out.log(chalk.bold(`Logs:\n`))
      this.out.log(getConsoleOutput(this.config.definitionDir, true, buffer))
    }

    return result
  }

  private injectEnvironment() {
    if (typeof this.fnDefinition.handler.code === 'object') {
      const { environment } = this.fnDefinition.handler.code!
      if (environment) {
        Object.assign(process.env, environment)
      }
    }
  }
}
