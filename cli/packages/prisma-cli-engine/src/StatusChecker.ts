import { Config } from './Config'
import * as crypto from 'crypto'
import * as path from 'path'
import { spawn } from 'child_process'
import * as fetch from 'isomorphic-fetch'
import * as fs from 'fs-extra'
import { Environment } from 'prisma-yml'
import * as os from 'os'
import { getIsGlobal } from './utils/isGlobal'

export class StatusChecker {
  config: Config
  env: Environment
  constructor(config: Config, env: Environment) {
    this.config = config
    this.env = env
  }
  checkStatus(command: string, args: any, flags: any, argv: any[]) {
    const source = 'CLI'
    const sourceVersion = this.config.version
    const eventName = 'command_triggered'
    const payload = JSON.stringify({
      command,
      args,
      flags,
      argv,
    })
    const auth = this.env.globalRC.cloudSessionKey
    const hashDate = new Date().toISOString()
    const mac = getMac()

    const fid = getFid()
    const globalBin = getIsGlobal()

    const message = JSON.stringify({
      source,
      sourceVersion,
      eventName,
      payload,
      auth,
      fid,
      globalBin,
      hashDate,
    })
    const secret = 'eshi4ohgai3eeHaih4Bifahhi'

    const hash = crypto
      .createHmac('sha256', secret)
      .update(message)
      .digest('hex')

    const query = `mutation(
      $input: StatsInput!
    ) {
      sendStats(
        data: $input
      )
    }`

    const options = {
      request: {
        query,
        variables: {
          input: {
            source,
            sourceVersion,
            eventName,
            payload,
            auth,
            fid,
            globalBin,
            hash,
            hashDate,
          },
        },
      },
      cachePath: this.config.requestsCachePath,
    }

    // Spawn a detached process, passing the options as an environment property

    // doJobs(options.cachePath, options.request)

    spawn(
      process.execPath,
      [path.join(__dirname, 'check.js'), JSON.stringify(options)],
      {
        detached: true,
        stdio: 'ignore',
      },
    ).unref()
  }
}

export async function doJobs(cachePath: string, request: any) {
  // use a '\n' separated list of stringified requests to make append only possible
  fs.appendFileSync(cachePath, JSON.stringify(request) + '\n')
  const requestsString = fs.readFileSync(cachePath, 'utf-8')
  const lines = requestsString.split('\n')
  const pendingRequests = lines
    .filter(r => r.trim() !== '')
    .map(r => JSON.parse(r))
  let linesToDelete = 0
  try {
    for (const job of pendingRequests) {
      await requestWithTimeout(job)
      linesToDelete++
    }
  } catch (e) {
    //
  }

  fs.writeFileSync(cachePath, lines.slice(linesToDelete).join('\n'))
  process.exit()
}

async function requestWithTimeout(input) {
  return new Promise(async (resolve, reject) => {
    setTimeout(() => {
      reject('Timeout')
    }, 5000)
    const result = await fetch(
      process.env.STATS_ENDPOINT || 'https://stats.prismagraphql.com',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(input),
      },
    )
    const json = await result.json()
    resolve(json)
  })
}

function getMac() {
  const interfaces = os.networkInterfaces()
  return Object.keys(interfaces).reduce((acc, key) => {
    if (acc) {
      return acc
    }
    const i = interfaces[key]
    const mac = i.find(a => a.mac !== '00:00:00:00:00:00')
    return mac ? mac.mac : null
  }, null)
}

let fidCache: string | null = null

export function getFid() {
  if (fidCache) {
    return fidCache
  }
  const mac = getMac()
  const fidSecret = 'yeiB6sooy6eedahgooj0shiez'
  const fid = mac
    ? crypto
        .createHmac('sha256', fidSecret)
        .update(mac)
        .digest('hex')
    : ''
  fidCache = fid
  return fid
}
