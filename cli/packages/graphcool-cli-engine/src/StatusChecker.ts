import { Config } from './Config'
import * as crypto from 'crypto'
import * as path from 'path'
import { Environment } from './Environment'
import { spawn } from 'child_process'
import * as fetch from 'isomorphic-fetch'
import * as fs from 'fs-extra'

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
    const platformToken = this.env.token
    const hashDate = new Date().toUTCString()
    const message = JSON.stringify({
      source,
      sourceVersion,
      eventName,
      payload,
      platformToken,
      hashDate,
    })
    const secret = 'oku0pa7ahcaPahg2Eeheeshun'

    const hash = crypto
      .createHmac('sha256', secret)
      .update(message)
      .digest('hex')

    const query = `mutation(
        $source: Source!
        $sourceVersion: String!
        $eventName: String!
        $payload: String
        $platformToken: String
        $hash: String!
        $hashDate: String!
      ) {
        sendStats(
          source: $source
          sourceVersion: $sourceVersion
          eventName: $eventName
          payload: $payload
          platformToken: $platformToken
          hash: $hash
          hashDate: $hashDate
        )
    }`

    const options = {
      request: {
        query,
        variables: {
          source,
          sourceVersion,
          eventName,
          payload,
          platformToken,
          hash,
          hashDate,
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
    const result = await fetch('https://stats.graph.cool', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(input),
    })
    const json = await result.json()
    resolve(json)
  })
}
