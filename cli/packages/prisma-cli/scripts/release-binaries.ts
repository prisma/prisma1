import * as spawn from '@expo/spawn-async'
import * as AWS from 'aws-sdk'
import axios from 'axios'
import * as createLogger from 'progress-estimator'
import * as os from 'os'
import * as fs from 'fs-extra'
import { gitCommitPush } from 'git-commit-push-via-github-api'
import { spawnSync } from 'child_process'
const token = process.env.GITHUB_TOKEN

// @ts-ignore
const logger = createLogger()

AWS.config.update({
  accessKeyId: process.env.AWS_ACCESS_KEY_ID,
  secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
})

axios.defaults.headers.common.Authorization = `Bearer ${token}`

const s3 = new AWS.S3({ params: { timeout: 6000000 } })

function uploadFiletoS3(bucket: string, filename: string, file: Buffer) {
  const s3Resp = s3
    .upload({
      Bucket: bucket,
      Key: filename,
      Body: file,
      ACL: 'public-read',
    })
    .promise()
  return logger(s3Resp, `Upload ${filename} to s3`, {
    estimate: 30000,
  })
}

async function createBinary(command: string, release: string) {
  await logger(
    spawn('npm', ['run', command]),
    `Creating binary using ${command} command`,
    {
      estimate: 60000,
    },
  )
  const tarFileName = `prisma-${release}-${command}.tar.gz`
  await logger(spawn('tar', ['-cvzf', tarFileName, 'prisma']), 'Creating tar', {
    estimate: 10000,
  })
  return tarFileName
}

async function brew(stableReleaseVersion: string) {
  console.log('Home Brew Release')

  const tarFileName = await createBinary('binary-osx', stableReleaseVersion)
  const shaResponse = spawnSync('shasum', ['-a', '256', tarFileName])
  const shaValue = shaResponse.stdout
    .toString()
    .split(' ')[0]
    .trim()
  console.log(`shasum -a 256 ${tarFileName} => ${shaValue}`)
  const fileData = await fs.readFile(tarFileName)

  const s3Resp = await uploadFiletoS3(
    'homebrew-prisma',
    `prisma-${stableReleaseVersion}.tar.gz`,
    fileData,
  )

  const uploadedBinaryURL = s3Resp.Location
  console.log(`uploaded binary url ${uploadedBinaryURL}`)
  let homebrewDefinition = ``
  const homeBrewTmp = `
class Prisma < Formula
  desc "Prisma turns your database into a realtime GraphQL API"
  homepage "https://github.com/prisma/prisma"
  url "https://s3-eu-west-1.amazonaws.com/homebrew-prisma/prisma-1.22.0.patch.1.tar.gz"
  sha256 "052cc310ab3eae8277e4d6fbf4848bc5c518af8e5165217a384bc26df82e63b9"
  version "1.22.0.patch.1"

  bottle :unneeded

  def install
    bin.install "prisma"
  end
end
  `
  homeBrewTmp.split(/\r?\n/).forEach(line => {
    if (line.includes('version')) {
      homebrewDefinition += `  version "${stableReleaseVersion}"${os.EOL}`
    } else if (line.includes('url')) {
      homebrewDefinition += `  url "${uploadedBinaryURL}"${os.EOL}`
    } else if (line.includes('sha256')) {
      homebrewDefinition += `  sha256 "${shaValue}"${os.EOL}`
    } else {
      homebrewDefinition += `${line}${os.EOL}`
    }
  })

  await gitCommitPush({
    owner: 'prisma',
    repo: 'homebrew-prisma',
    token,
    files: [{ path: 'prisma.rb', content: homebrewDefinition }],
    fullyQualifiedRef: 'heads/automated-pr-branch',
    commitMessage: `bump version to ${stableReleaseVersion}`,
  })

  const pullRes = await axios.post(
    'https://api.github.com/repos/prisma/homebrew-prisma/pulls',
    {
      title: `Automated PR for version ${stableReleaseVersion}`,
      head: 'automated-pr-branch',
      base: 'master',
      body: ' Automated PR generated via script',
      maintainer_can_modify: true,
    },
  )
  console.log(
    `Pull Request created at ${
      pullRes.data.html_url
    }. Merge this to complete the release`,
  )
}

async function linux(stableReleaseVersion: string) {
  console.log('releasing linux')
  const tarFileName = await createBinary('binary-linux', stableReleaseVersion)
  const fileData = await fs.readFile(tarFileName)

  const s3Resp = await uploadFiletoS3(
    'curl-linux',
    `prisma-${stableReleaseVersion}.tar.gz`,
    fileData,
  )

  const uploadedBinaryURL = s3Resp.Location

  console.log('Linux binary uploaded to ' + uploadedBinaryURL)
}

function checkEnvs() {
  const missing = []
  const map = {
    GITHUB_TOKEN: process.env.GITHUB_TOKEN,
    AWS_ACCESS_KEY_ID: process.env.AWS_ACCESS_KEY_ID,
    AWS_SECRET_ACCESS_KEY: process.env.AWS_SECRET_ACCESS_KEY,
  }
  Object.keys(map).forEach(x => {
    if (!map[x]) {
      missing.push(x)
    }
  })
  if (missing.length > 0) {
    let errorMsg = `Environment vars`
    missing.forEach(x => (errorMsg += ` ${x},`))
    errorMsg = errorMsg.substring(0, errorMsg.length - 1)
    errorMsg += ' are missing'
    console.error(errorMsg)
    process.exit(1)
  }
}

async function main() {
  checkEnvs()
  const tData = await logger(
    axios.get('https://api.github.com/repos/prisma/prisma/releases'),
    'Fetching version',
    {
      estimate: 800,
    },
  )
  const stableReleaseVersion = tData.data.filter(
    node => !node.tag_name.includes('alpha') && !node.tag_name.includes('beta'),
  )[0].tag_name
  console.log(`Version to publish: ${stableReleaseVersion}`)

  await brew(stableReleaseVersion)
  await linux(stableReleaseVersion)
}
main()
