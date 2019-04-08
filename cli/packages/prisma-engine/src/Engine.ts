import { spawn, ChildProcess } from 'child_process'
import { request } from 'graphql-request'
import * as fs from 'fs'
import * as path from 'path'
import * as net from 'net'
import { safeLoad } from 'js-yaml'

interface EngineConfig {
  prismaConfig?: string
  debug?: boolean
  managementApiEnabled?: boolean
}

/**
 * Node.js based wrapper to run the Prisma binary
 */
export class Engine {
  prismaConfig: string
  port?: number
  debug: boolean
  child?: ChildProcess
  /**
   * exiting is used to tell the .on('exit') hook, if the exit came from our script.
   * As soon as the Prisma binary returns a correct return code (like 1 or 0), we don't need this anymore
   */
  exiting: boolean = false
  managementApiEnabled: boolean = false
  constructor({
    prismaConfig,
    debug,
    managementApiEnabled,
  }: EngineConfig = {}) {
    this.prismaConfig = prismaConfig || this.getPrismaYml()
    this.debug = debug || false
    this.managementApiEnabled = managementApiEnabled || false
  }

  /**
   * Use the port 0 trick to get a new port
   */
  getFreePort(): Promise<number> {
    return new Promise((resolve, reject) => {
      const server = net.createServer(s => s.end(''))
      server.listen(0, () => {
        const address = server.address()
        if (!address) {
          throw new Error(
            'Could not find free port. Please contact the Prisma team',
          )
        }
        const port =
          typeof address === 'string'
            ? parseInt(address.split(':').slice(-1)[0], 10)
            : address.port
        server.close(e => {
          if (e) {
            throw e
          }
          resolve(port)
        })
      })
    })
  }

  /**
   * Resolve the prisma.yml
   */
  getPrismaYml() {
    const prismaYmlPath = this.getPrismaYmlPath()
    return fs.readFileSync(prismaYmlPath, 'utf-8')
  }

  getDatamodelPath() {
    const yml = safeLoad(this.prismaConfig)
    return path.resolve(yml.datamodel)
  }

  /**
   * Resolve the prisma.yml path
   */
  getPrismaYmlPath() {
    if (fs.existsSync('prisma.yml')) {
      return 'prisma.yml'
    }

    const prismaPath = path.join(process.cwd(), 'prisma/prisma.yml')
    if (fs.existsSync(prismaPath)) {
      return prismaPath
    }

    const parentPath = path.join(process.cwd(), '../prisma.yml')
    if (fs.existsSync(parentPath)) {
      return parentPath
    }

    throw new Error(`Could not find prisma.yml`)
  }

  /**
   * Replace the port in the Prisma Config
   */
  generatePrismaConfig() {
    return `managementApiEnabled: ${this.managementApiEnabled}\nport: ${
      this.port
    }\n${this.prismaConfig}`
  }

  /**
   * Starts the engine
   */
  async start() {
    this.port = await this.getFreePort()
    const PRISMA_CONFIG = this.generatePrismaConfig()
    if (this.debug) {
      console.log('PRISMA_CONFIG')
      console.log(PRISMA_CONFIG)
      console.log('\n')
    }
    this.child = spawn(path.join(__dirname, '../prisma'), [], {
      env: {
        PRISMA_CONFIG,
        PRISMA_SCHEMA_PATH: this.getDatamodelPath(),
      },
      detached: false,
      stdio: this.debug ? 'inherit' : 'pipe',
    })
    this.child.on('error', e => {
      throw e
    })
    this.child.on('exit', code => {
      if (code !== 0 && !this.exiting) {
        const debugString = this.debug
          ? ''
          : 'Please enable "debug": true in the Engine constructor to get more insights.'
        throw new Error(`Child exited with code ${code}${debugString}`)
      }
    })
    await new Promise(r => setTimeout(r, 500))
  }

  /**
   * If Prisma runs, stop it
   */
  stop() {
    if (this.child) {
      this.exiting = true
      this.child.kill()
    }
  }

  /**
   *
   * @param url relative url like /service/stage
   * @param query query to send to the prisma service
   * @param variables optional: variables
   */
  async request(query, variables?: any, url = '') {
    return request(`http://localhost:${this.port}${url}`, query, variables)
  }
}
