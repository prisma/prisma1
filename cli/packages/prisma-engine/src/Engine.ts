import { spawn, ChildProcess } from 'child_process'
import { request } from 'graphql-request'
import * as fs from 'fs'
import * as path from 'path'
import * as net from 'net'

interface EngineConfig {
  prismaConfig?: string
  debug?: boolean
  binaryPath?: string
}

/**
 * Node.js based wrapper to run the Prisma binary
 */
export class Engine {
  prismaConfig: string
  port?: number
  binaryPath?: string
  debug: boolean
  child?: ChildProcess
  /**
   * exiting is used to tell the .on('exit') hook, if the exit came from our script.
   * As soon as the Prisma binary returns a correct return code (like 1 or 0), we don't need this anymore
   */
  exiting: boolean = false
  constructor({ prismaConfig, debug, binaryPath }: EngineConfig = {}) {
    this.prismaConfig = prismaConfig || this.getPrismaYml()
    this.debug = debug || false
    this.binaryPath = binaryPath
  }

  /**
   * Use the port 0 trick to get a new port
   */
  getFreePort(): Promise<number> {
    return new Promise((resolve, reject) => {
      const server = net.createServer(s => s.end(''))
      server.listen(0, () => {
        const address = server.address()
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
    return `port: ${this.port}\n${this.prismaConfig}`
  }

  /**
   * Starts the engine
   */
  async start() {
    this.port = await this.getFreePort()
    const PRISMA_CONFIG = this.generatePrismaConfig()
    const BINARY_PATH = this.binaryPath
      ? this.binaryPath
      : path.join(__dirname, '../prisma')
    this.child = spawn(BINARY_PATH, [], {
      env: {
        PRISMA_CONFIG,
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
    await new Promise(r => setTimeout(r, 100))
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
