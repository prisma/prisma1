import { GraphQLClient } from 'graphql-request'
import { spawnSync } from 'child_process'
import { chdir } from 'process'
import * as fs from 'fs'
import * as os from 'os'

const token = '' // TODO: Get token from environment
const endpoint = 'https://api.github.com/graphql'
const homebrewRepositoryURL = 'https://github.com/prisma/homebrew-prisma'
const client = new GraphQLClient(endpoint, {
  headers: {
    Authorization: `Bearer ${token}`,
  },
})

interface ResponseRepository {
  repository: {
    releases: {
      nodes: Array<{
        tag: {
          name: string
        }
      }>
    }
  }
}

async function main() {
  const cloneReponse = spawnSync('git', ['clone', homebrewRepositoryURL])
  console.log(`cloning ${homebrewRepositoryURL}`)
  console.log(cloneReponse.stdout.toString())

  const stableReleaseVersion = (await client.request<
    ResponseRepository
  >(/* GraphQL */ `
    query Repository {
      repository(name: "prisma", owner: "prisma") {
        url
        releases(first: 10, orderBy: { field: CREATED_AT, direction: DESC }) {
          nodes {
            tag {
              name
            }
          }
        }
      }
    }
  `)).repository.releases.nodes.filter(
    node => !node.tag.name.includes('alpha') && !node.tag.name.includes('beta'),
  )[0].tag.name
  console.log(`Version to publish: ${stableReleaseVersion}`)

  chdir('homebrew-prisma')

  // TODO: Generate binary dynamically! For development, assuming that
  // binary "prisma" exists

  const tarFileName = `prisma-${stableReleaseVersion}.tar.gz`
  const tarResponse = spawnSync('tar', ['-czf', tarFileName, '../prisma'])
  console.log('making tar', tarResponse.stdout.toString())

  const shaResponse = spawnSync('shasum', ['-a', '256', tarFileName])
  const shaValue = shaResponse.stdout
    .toString()
    .split(' ')[0]
    .trim()
  console.log(`shasum -a 256 ${tarFileName} => ${shaValue}`)

  // TODO: Upload binary to S3. For development, assuming that
  // this is the URL
  const uploadedBinaryURL = `https://s3-eu-west-1.amazonaws.com/homebrew-prisma/prisma-${stableReleaseVersion}.tar.gz`
  console.log(`uploaded binary url ${uploadedBinaryURL}`)

  let homebrewDefinition = ``
  fs.readFileSync('prisma.rb', 'utf-8')
    .split(/\r?\n/)
    .forEach(line => {
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
  console.log(homebrewDefinition)

  fs.writeFileSync('prisma.rb', homebrewDefinition, {
    encoding: 'utf-8',
  })

  // TODO: Create a PR/push to the repository programmatically.
}

main()
