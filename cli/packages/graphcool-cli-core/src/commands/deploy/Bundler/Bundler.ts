import Deploy from '../index'
import {
  ProjectDefinitionClass,
  ExternalFiles,
  ExternalFile,
  Config,
  Client,
  Output,
} from 'graphcool-cli-engine'
import * as archiver from 'archiver'
import * as fs from 'fs-extra'
import * as path from 'path'
import * as multimatch from 'multimatch'
import { FunctionDefinition } from 'graphcool-json-schema'
import TypescriptBuilder from './TypescriptBuilder'
const debug = require('debug')('bundler')
import * as fetch from 'node-fetch'
import * as globby from 'globby'
import * as chalk from 'chalk'

const patterns = ['**/*.graphql', '**/graphcool.yml'].map(i => `!${i}`)
patterns.unshift('**/*')

export interface FunctionTuple {
  name: string
  fn: FunctionDefinition
}

export default class Bundler {
  definition: ProjectDefinitionClass
  config: Config
  client: Client
  out: Output
  projectId: string
  buildDir: string
  zipPath: string
  constructor(cmd: Deploy, projectId: string) {
    this.out = cmd.out
    this.definition = cmd.definition
    this.config = cmd.config
    this.client = cmd.client
    this.projectId = projectId
    this.buildDir = path.join(this.config.definitionDir, '.build/')
    this.zipPath = path.join(this.config.definitionDir, 'build.zip')
    debug(this.zipPath)
  }

  async bundle(): Promise<ExternalFiles> {
    if (this.functions.length === 0) {
      return {}
    }

    if (
      !fs.pathExistsSync(
        path.join(this.config.definitionDir, 'package.json'),
      )
    ) {
      this.out.warn(`You have defined functions but no package.json has been found.`)
    }

    if (
      !fs.pathExistsSync(path.join(this.config.definitionDir, 'node_modules'))
    ) {
      this.out.warn(`You have defined functions but no node_modules has been found. Please run ${chalk.green('npm install')}`)
    }

    if (
      !fs.pathExistsSync(path.join(this.config.definitionDir, 'node_modules')) ||
      !fs.pathExistsSync(
        path.join(this.config.definitionDir, 'package.json'),
      )
    ) {
      this.out.warn(`Note, that the new function runtime doesn't inject
node modules automatically anymore and you need to
install them before deploying
Read more here: https://github.com/graphcool/graphcool/issues/800
`)
    }
    this.out.action.start('Bundling functions')


    let before = Date.now()
    fs.removeSync(this.buildDir)
    fs.mkdirpSync(this.buildDir)
    const builder = new TypescriptBuilder(
      this.config.definitionDir,
      this.buildDir,
    )
    const zip = archiver('zip')
    const write = fs.createWriteStream(this.zipPath)
    zip.pipe(write)
    zip.on('error', err => {
      this.out.error('Error while zipping build: ' + err)
    })
    const files = await globby(['**/*', '!.build', '!*.zip', '!build'])
    files.forEach(file => {
      zip.file(file, { name: file })
    })
    await builder.compile(this.fileNames)
    this.generateEnvFiles()
    this.generateHandlerFiles()
    zip.directory(this.buildDir, false)
    zip.finalize()

    await new Promise(r => write.on('close', () => r()))

    const url = await this.upload()

    debug('bundled', Date.now() - before)
    this.out.action.stop(this.prettyTime(Date.now() - before))

    return this.getExternalFiles(url)
  }

  private prettyTime(time: number): string {
    let output = ''
    if (time > 1000) {
      output = (Math.round(time / 100) / 10).toFixed(1) + 's'
    } else {
      output = time + 'ms'
    }
    return chalk.cyan(output)
  }

  cleanBuild(): Promise<void> {
    return fs.remove(this.buildDir)
  }

  async upload(): Promise<string> {
    const stream = fs.createReadStream(this.zipPath)
    const stats = fs.statSync(this.zipPath)
    const url = await this.client.getDeployUrl(this.projectId)
    debug('uploading to', url)
    let body = stream
    const res = await fetch(url, {
      method: 'PUT',
      body,
      headers: {
        'Content-Length': stats.size,
        'Content-Type': 'application/zip',
      },
    })
    const text = await res.text()
    debug(text)

    return url
  }

  getExternalFiles(url: string): ExternalFiles {
    return this.functions.reduce((acc, fn) => {
      const src =
        typeof fn.fn.handler.code === 'string'
          ? fn.fn.handler.code
          : fn.fn.handler.code!.src
      const buildFileName = this.getBuildFileName(src)
      const lambdaHandler = this.getLambdaHandler(buildFileName)
      const devHandler = this.getDevHandlerPath(buildFileName)

      const externalFile = {
        url,
        lambdaHandler,
        devHandler,
      }
      return { ...acc, [src]: externalFile }
    }, {})
  }

  generateEnvFiles() {
    this.functions.forEach(fn => {
      if (typeof fn.fn.handler.code === 'object') {
        const src = fn.fn.handler.code!.src
        const buildFileName = this.getBuildFileName(src)
        const envPath = path.join(this.buildDir, this.getEnvPath(buildFileName))
        if (fn.fn.handler.code!.environment) {
          const env = JSON.stringify(fn.fn.handler.code!.environment)
          fs.writeFileSync(envPath, env)
        }
      }
    })
  }

  generateHandlerFiles() {
    this.functions.forEach(fn => {
      const src =
        typeof fn.fn.handler.code === 'string'
          ? fn.fn.handler.code
          : fn.fn.handler.code!.src
      const buildFileName = path.join(this.buildDir, this.getBuildFileName(src))
      const lambdaHandlerPath = this.getLambdaHandlerPath(buildFileName)
      const devHandlerPath = this.getDevHandlerPath(buildFileName)
      const bylinePath = this.getBylinePath(buildFileName)
      fs.copySync(
        path.join(__dirname, './proxies/lambda.js'),
        lambdaHandlerPath,
      )
      fs.copySync(path.join(__dirname, './proxies/dev.js'), devHandlerPath)
      fs.copySync(path.join(__dirname, './proxies/byline.js'), bylinePath)
    })
  }

  get functions(): Array<FunctionTuple> {
    const functions = this.definition.definition!.modules[0].definition!
      .functions

    if (!functions) {
      return []
    } else {
      return Object.keys(functions)
        .filter(name => functions[name].handler.code)
        .map(name => {
          return {
            name,
            fn: functions[name],
          }
        })
    }
  }

  get fileNames(): string[] {
    return this.functions.map(fn =>
      path.join(
        this.config.definitionDir,
        typeof fn.fn.handler.code === 'string'
          ? fn.fn.handler.code
          : fn.fn.handler.code!.src,
      ),
    )
  }

  getBuildFileName = (src: string) => path.join(src.replace(/\.ts$/, '.js'))
  getLambdaHandlerPath = (fileName: string) =>
    fileName.slice(0, fileName.length - 3) + '-lambda.js'
  getLambdaHandler = (fileName: string) =>
    fileName.slice(0, fileName.length - 3) + '-lambda.handle'
  getDevHandlerPath = (fileName: string) =>
    fileName.slice(0, fileName.length - 3) + '-dev.js'
  getBylinePath = (fileName: string) => path.dirname(fileName) + '/byline.js'
  getEnvPath = (fileName: string) =>
    fileName.slice(0, fileName.length - 3) + '.env.json'
}
