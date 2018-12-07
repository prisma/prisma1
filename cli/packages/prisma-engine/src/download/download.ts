// Inspired by https://github.com/zeit/now-cli/blob/canary/download/src/index.js
// Native
import * as fs from 'fs'
import * as path from 'path'
import * as zlib from 'zlib'

// Packages
import * as onDeath from 'death'
import fetch from 'node-fetch'
import * as retry from 'async-retry'

// Utils
import {
  disableProgress,
  enableProgress,
  info,
  showProgress,
  warn,
} from './log'
import plusxSync from './chmod'

const { platform } = process

const packageDir = path.join(__dirname, '../..')
const packageJSON = require(path.join(packageDir, 'package.json'))

const targetWin32 = path.join(__dirname, 'prisma.exe')
const prisma = path.join(__dirname, '../../prisma')
const target = platform === 'win32' ? targetWin32 : prisma
const partial = target + '.partial'

/**
 * TODO: Check if binary already exists and if checksum is the same!
 */
async function download() {
  try {
    fs.writeFileSync(
      prisma,
      '#!/usr/bin/env node\n' +
        'console.log("Please wait until the \'prisma\' installation completes!")\n',
    )
  } catch (err) {
    if (err.code === 'EACCES') {
      warn(
        'Please try installing Now CLI again with the `--unsafe-perm` option.',
      )
      info('Example: `npm i -g --unsafe-perm prisma`')

      process.exit()
    }

    throw err
  }

  onDeath(() => {
    fs.writeFileSync(
      prisma,
      '#!/usr/bin/env node\n' +
        'console.log("The \'prisma\' installation did not complete successfully.")\n' +
        'console.log("Please run \'npm i -g prisma\' to reinstall!")\n',
    )
    process.exit()
  })

  info('For the source code, check out: https://github.com/prisma/prisma')

  // Print an empty line
  console.log('')

  await retry(
    async () => {
      enableProgress('Downloading Prisma Binary ' + packageJSON.version)
      showProgress(0)

      try {
        // const name = platformToName[platform]
        // const url = `https://github.com/zeit/now-cli/releases/download/${
        //   packageJSON.version
        // }/${name}.gz`
        const url =
          'https://s3-eu-west-1.amazonaws.com/curl-linux/prisma-native.gz'
        const resp = await fetch(url, { compress: false })

        if (resp.status !== 200) {
          throw new Error(resp.statusText + ' ' + url)
        }

        const size = resp.headers.get('content-length')
        const ws = fs.createWriteStream(partial)

        await new Promise((resolve, reject) => {
          let bytesRead = 0

          resp.body.on('error', reject).on('data', chunk => {
            bytesRead += chunk.length

            if (size) {
              showProgress((100 * bytesRead) / size)
            }
          })

          const gunzip = zlib.createGunzip()

          gunzip.on('error', reject)

          resp.body.pipe(gunzip).pipe(ws)

          ws.on('error', reject).on('close', () => {
            showProgress(100)
            resolve()
          })
        })
      } finally {
        disableProgress()
      }
    },
    {
      retries: 10,
      onRetry: err => console.error(err),
    },
  )

  fs.renameSync(partial, target)
}

async function main() {
  await download()

  plusxSync(prisma)
}

main().catch(e => {
  console.error(e)
  process.exit(2)
})
