import Deploy from '../index'
import {ProjectDefinitionClass, ExternalFiles, ExternalFile, Config, Client, Output} from 'graphcool-cli-engine'
import * as archiver from 'archiver'
import * as fs from 'fs-extra'
import * as path from 'path'
import * as multimatch from 'multimatch'
import {FunctionDefinition} from 'graphcool-json-schema'
import TypescriptBuilder from './TypescriptBuilder'
import * as FormData from 'form-data'
const debug = require('debug')('bundler')
import * as fetch from 'node-fetch'
import {PassThrough} from 'stream'
import * as request from 'request'
import * as os from 'os'
import * as globby from 'globby'

const patterns = [
  '**/*.graphql',
  '**/graphcool.yml'
].map(i => `!${i}`)
patterns.unshift('**/*')

export interface FunctionTuple {name: string, fn: FunctionDefinition}

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

    const before = Date.now()
    debug('bundling')
    fs.removeSync(this.buildDir)
    fs.mkdirpSync(this.buildDir)
    const builder = new TypescriptBuilder(this.config.definitionDir, this.buildDir)
    const zip = archiver('zip')
    const write = fs.createWriteStream(this.zipPath)
    zip.pipe(write)
    zip.on('error', (err) => {
      this.out.error('Error while zipping build: ' + err)
    })
    const files = await globby(['**/*', '!.build', '!*.zip', '!build'])
    files.forEach(file => {
      zip.file(file, {name: file})
    })
    await builder.compile(this.fileNames)
    this.generateEnvFiles()
    this.generateHandlerFiles()
    zip.directory(this.buildDir, false)
    zip.finalize()

    await new Promise(r => write.on('close', () => r()))

    const url = await this.upload()

    debug('bundled', Date.now() - before)

    return this.getExternalFiles(url)
  }

  cleanBuild(): Promise<void> {
    return fs.remove(this.buildDir)
  }

  async upload(): Promise<string> {
    const stream = fs.createReadStream(this.zipPath)
    const stats = fs.statSync(this.zipPath)
    const url = await this.client.getDeployUrl(this.projectId)
    // const url = 'http://127.0.0.1:60050/functions/files/WHATEVER/WHATEVER'
    // const url = 'http://127.0.0.1:3000/upload'
    // const url = 'https://requestb.in/rmmp7xrm'
    debug('uploading to', url)
    let body = stream
    if (url.includes('127.0.0.1') || url.includes('localhost')) {
      const form = new FormData()
      form.append('file', stream)
      body = form
    }
    const res = await fetch(url, { method: 'PUT', body, headers: {
      'Content-Length': stats.size
    } })
    const text = await res.text()
    debug(text)
    // await new Promise((resolve, reject) => {
    //   stream.pipe(
    //     request.put(url, (err, res) => {
    //       console.log(err, res.body)
    //       resolve(res)
    //     })
    //   )
    // })

    return url
  }

  getExternalFiles(url: string): ExternalFiles {
    return this.functions.reduce((acc, fn) => {
      const src = fn.fn.handler.code!.src
      const buildFileName = this.getBuildFileName(src)
      const lambdaHandler = this.getLambdaHandler(buildFileName)
      const devHandler = this.getDevHandlerPath(buildFileName)

      const externalFile = {
        url,
        lambdaHandler,
        devHandler,
      }
      return {...acc, [src]: externalFile}
    }, {})
  }

  generateEnvFiles() {
    this.functions.forEach(fn => {
      const src = fn.fn.handler.code!.src
      const buildFileName = this.getBuildFileName(src)
      const envPath = path.join(this.buildDir, this.getEnvPath(buildFileName))
      if (fn.fn.handler.code!.environment) {
        const env = JSON.stringify(fn.fn.handler.code!.environment)
        fs.writeFileSync(envPath, env)
      }
    })
  }

  generateHandlerFiles() {
    this.functions.forEach(fn => {
      const src = fn.fn.handler.code!.src
      const buildFileName = path.join(this.buildDir, this.getBuildFileName(src))
      const lambdaHandlerPath = this.getLambdaHandlerPath(buildFileName)
      const devHandlerPath = this.getDevHandlerPath(buildFileName)
      const bylinePath = this.getBylinePath(buildFileName)
      fs.copySync(path.join(__dirname, './proxies/lambda.js'), lambdaHandlerPath)
      fs.copySync(path.join(__dirname, './proxies/dev.js'), devHandlerPath)
      fs.copySync(path.join(__dirname, './proxies/byline.js'), bylinePath)
    })
  }

  async cpFiles(src: string, dest: string) {
    await fs.copy(src, dest, {
      filter: this.validFile
    })
  }

  validFile(filePath: string) {
    return multimatch(filePath, patterns).length > 0
  }

  get functions(): Array<FunctionTuple> {
    const functions = this.definition.definition!.modules[0].definition!.functions

    if (!functions) {
      return []
    } else {
      return Object.keys(functions)
        .filter(name => functions[name].handler.code)
        .map(name => {
          return {
            name,
            fn: functions[name]
          }
        })
    }
  }

  get fileNames(): string[] {
    return this.functions.map(fn => path.join(this.config.definitionDir, fn.fn.handler.code!.src))
  }

  getBuildFileName = (src: string) => path.join(src.replace(/\.ts$/, '.js'))
  getLambdaHandlerPath = (fileName: string) => fileName.slice(0, fileName.length - 3) + '-lambda.js'
  getLambdaHandler = (fileName: string) => fileName.slice(0, fileName.length - 3) + '-lambda.handle'
  getDevHandlerPath = (fileName: string) => fileName.slice(0, fileName.length - 3) + '-dev.js'
  getBylinePath = (fileName: string) => path.dirname(fileName) + '/byline.js'
  getEnvPath = (fileName: string) => fileName.slice(0, fileName.length - 3) + '.env.json'

}