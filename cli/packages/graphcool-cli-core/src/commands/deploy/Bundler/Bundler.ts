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
import {difference, flatMap} from 'lodash'
import 'isomorphic-fetch'
import * as globby from 'globby'
import chalk from 'chalk'

const patterns = ['**/*.graphql', '**/graphcool.yml'].map(i => `!${i}`)
patterns.unshift('**/*')

const defaultGlobbyOptions = {
  dot: true,
  silent: true,
  follow: true,
  nosort: true,
  mark: true
}

export default class Bundler {
  definition: ProjectDefinitionClass
  config: Config
  client: Client
  out: Output
  projectId: string
  buildDir: string
  zipPath: string
  dotBuildDir: string
  constructor(cmd: Deploy, projectId: string) {
    this.out = cmd.out
    this.definition = cmd.definition
    this.config = cmd.config
    this.client = cmd.client
    this.projectId = projectId
    this.dotBuildDir = path.join(this.config.definitionDir, '.build/')
    this.buildDir = path.join(this.dotBuildDir, 'dist/')
    this.zipPath = path.join(this.dotBuildDir, 'build.zip')
    debug(this.zipPath)
  }

  async getIsDir(filePath: string): Promise<{filePath: string, isDir: boolean}> {
    const stat = await fs.stat(filePath)
    return {
      filePath,
      isDir: stat.isDirectory()
    }
  }

  async bundle(): Promise<ExternalFiles> {
    if (this.definition.functions.length === 0) {
      return {}
    }

    this.out.action.start('Bundling functions')

    let before = Date.now()
    fs.removeSync(this.dotBuildDir)
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
    const files = await globby(['**/*', '!.build', '!*.zip', '!build'], defaultGlobbyOptions)
    const filesToAdd = difference(files, this.shortFileNamesBlacklist)
    filesToAdd.forEach(file => {
      zip.file(file, { name: file })
    })
    debug('added files', filesToAdd)
    const createdFiles = await builder.compile(this.fullFileNames)
    debug('converted files', this.fullFileNames)
    debug('createdFiles', createdFiles)
    this.generateEnvFiles()
    this.generateHandlerFiles()
    const distFiles = await globby(['**/*', '!.build', '!*.zip'], {...defaultGlobbyOptions, cwd: this.buildDir})
    distFiles.forEach(file => {
      const fileName = path.join(this.buildDir, file)
      debug('adding', fileName, file)
      zip.file(fileName, { name: file })
    })
    debug('added build files', distFiles)
    zip.finalize()

    await new Promise(r => write.on('close', () => r()))
    await new Promise(r => setTimeout(r, 100))

    const url = await this.upload()

    debug('bundled', Date.now() - before)
    this.out.action.stop(this.prettyTime(Date.now() - before))
    if (!this.config.debug) {
      fs.removeSync(this.dotBuildDir)
    }

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
    return this.definition.functions.reduce((acc, fn) => {
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
    this.definition.functions.forEach(fn => {
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
    this.definition.functions.forEach(fn => {
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

  get shortFileNamesBlacklist(): string[] {
    return flatMap(this.definition.functions.map(fn =>
        typeof fn.fn.handler.code === 'string'
          ? fn.fn.handler.code
          : fn.fn.handler.code!.src,
    )
      .map(n => n.startsWith('./') ? n.slice(2) : n)
      .map(n => n.endsWith('.ts') ? [n.slice(0, n.length - 2) + '.js', n] : n))
  }

  get fullFileNames(): string[] {
    return this.definition.functions.map(fn =>
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
