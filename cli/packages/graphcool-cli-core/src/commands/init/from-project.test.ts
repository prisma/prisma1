import * as nock from 'nock'
import { Config } from 'graphcool-cli-engine'
import Init from './'
import { definitionWithModule } from './mock'
import * as fs from 'fs-extra'
import * as path from 'path'

afterAll(() => {
  nock.cleanAll()
})

describe('init from project', () => {
  test('test project', async () => {

    nock('https://api.graph.cool:443', {"encodedQueryParams":true})
      .post('/system', {"query":"{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }"})
      .reply(200, {"data":{"viewer":{"user":{"id":"cj84c8sxb14ze0180pxb30q9r","email":"cli-tests@graph.cool"}}}}, [ 'Content-Type',
        'application/json',
        'Content-Length',
        '94',
        'Connection',
        'close',
        'Date',
        'Thu, 28 Sep 2017 14:45:17 GMT',
        'Request-Id',
        'eu-west-1:system:cj84kn8f303vp01509hng4dmq',
        'Server',
        'akka-http/10.0.8',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 279cefb37a6695aa72c905285954d61d.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        'JbT4-GfU5zEUNj_NQpHuYzVr7PQ3xf8OJakMDSC_6XLGgA-qimY0Pw==' ]);


    nock('https://api.graph.cool:443', {"encodedQueryParams":true})
      .post('/system', {"query":"      mutation addProject($name: String!, $alias: String, $region: Region, $config: String) {\n        addProject(input: {\n          name: $name,\n          alias: $alias,\n          region: $region,\n          clientMutationId: \"static\"\n          config: $config\n        }) {\n          project {\n            ...RemoteProject\n          }\n        }\n      }\n      \n  fragment RemoteProject on Project {\n    id\n    name\n    schema\n    alias\n    region\n    projectDefinitionWithFileContent\n  }\n\n      ","variables":{"name":"Project with Init from Project","region":"EU_WEST_1","config":"{\"modules\":[{\"name\":\"\",\"content\":\"types: ./types.graphql\\nmodules:\\n  github: modules/github/graphcool.yml\\npermissions:\\n  - operation: '*'\\nrootTokens: []\\n\",\"files\":{\"./types.graphql\":\"# This file contains the GraphQL Types\\n# Graphcool has one special type, the File type:\\n\\n# type File implements Node {\\n#   contentType: String!\\n#   createdAt: DateTime!\\n#   id: ID! @isUnique\\n#   name: String!\\n#   secret: String! @isUnique\\n#   size: Int!\\n#   updatedAt: DateTime!\\n#   url: String! @isUnique\\n# }\\n\\n# All types need to have an id field defined like this:\\n\\n# type User implements Node {\\n#   id: ID! @isUnique\\n# }\\n\\n\"}},{\"name\":\"github\",\"content\":\"types: ./types.graphql\\nfunctions:\\n  github-authentication:\\n    handler:\\n      code:\\n        src: ./code/github-authentication.js\\n        environment: {}\\n    type: resolver\\n    schema: ./schemas/github-authentication.graphql\\nrootTokens:\\n  - github-authentication\\n\",\"files\":{\"./types.graphql\":\"type GithubUser {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  githubUserId: String @isUnique\\n  updatedAt: DateTime!\\n}\\n\",\"./code/github-authentication.js\":\"const fromEvent = require('graphcool-lib').fromEvent\\n\\n// read Github credentials from environment variables\\nconst client_id = process.env.CLIENT_ID\\nconst client_secret = process.env.CLIENT_SECRET\\n\\nmodule.exports = function (event) {\\n  console.log(event)\\n\\n  if (!process.env.CLIENT_ID || !process.env.CLIENT_SECRET) {\\n    console.log('Please provide a valid client id and secret!')\\n    return { error: 'Github Authentication not configured correctly.'}\\n  }\\n\\n  if (!event.context.graphcool.pat) {\\n    console.log('Please provide a valid root token!')\\n    return { error: 'Github Authentication not configured correctly.'}\\n  }\\n\\n  const code = event.data.githubCode\\n  const graphcool = fromEvent(event)\\n  const api = graphcool.api('simple/v1')\\n\\n  function getGithubToken () {\\n    console.log('Getting access token...')\\n\\n    return fetch('https://github.com/login/oauth/access_token', {\\n      method: 'POST',\\n      headers: {\\n        'Content-Type': 'application/json',\\n        'Accept': 'application/json'\\n      },\\n      body: JSON.stringify({\\n        client_id,\\n        client_secret,\\n        code\\n      })\\n    })\\n    .then(data => data.json())\\n    .then(json => {\\n      if (json.error) {\\n        throw new Error(json)\\n      } else {\\n        console.log(json)\\n\\n        return json.access_token\\n      }\\n    })\\n    .catch(error => {\\n      console.log(error)\\n\\n      return Promise.reject({ message: 'Error while authenticating with Github' })\\n    })\\n  }\\n\\n  function getGithubAccountData (githubToken) {\\n    console.log('Getting account data...')\\n\\n    return fetch(`https://api.github.com/user?access_token=${githubToken}`)\\n      .then(response => response.json())\\n      .then(json => {\\n        if (json.error) {\\n          throw new Error(json)\\n        } else {\\n          console.log(json)\\n\\n          return json\\n        }\\n      })\\n      .catch(error => {\\n        console.log(error)\\n\\n        return Promise.reject({ message: 'Error while getting Github user data' })\\n      })\\n  }\\n\\n  function getGraphcoolUser (githubUser) {\\n    console.log('Getting Graphcool user...')\\n\\n    return api.request(`\\n      query {\\n        GithubUser(githubUserId: \\\"${githubUser.id}\\\") {\\n          id\\n        }\\n      }\\n    `)\\n    .then(response => {\\n      if (response.error) {\\n        throw new Error(response)\\n      } else {\\n        console.log(response)\\n\\n        return response.GithubUser\\n      }\\n    })\\n    .catch(error => {\\n      console.log(error)\\n\\n      return Promise.reject({ message: 'Error while getting Graphcool user' })\\n    })\\n  }\\n\\n  function createGraphcoolUser (githubUser) {\\n    console.log('Creating Graphcool user...')\\n\\n    return api.request(`\\n      mutation {\\n        createGithubUser(\\n          githubUserId:\\\"${githubUser.id}\\\"\\n        ) {\\n          id\\n        }\\n      }\\n    `)\\n    .then(response => {\\n      if (response.error) {\\n        throw new Error(response)\\n      } else {\\n        console.log(response)\\n\\n        return response.createGithubUser.id\\n      }\\n    })\\n    .catch(error => {\\n      console.log(error)\\n\\n      return Promise.reject({ message: 'Error while creating Graphcool user' })\\n    })\\n  }\\n\\n  function generateGraphcoolToken (graphcoolUserId) {\\n    return graphcool.generateAuthToken(graphcoolUserId, 'GithubUser')\\n      .catch(error => {\\n        console.log(error)\\n\\n        return Promise.reject({ message: 'Error while generating token' })\\n      })\\n  }\\n\\n  return getGithubToken()\\n    .then(githubToken => {\\n      return getGithubAccountData(githubToken)\\n        .then(githubUser => {\\n          return getGraphcoolUser(githubUser).then(graphcoolUser => {\\n            if (graphcoolUser === null) {\\n              return createGraphcoolUser(githubUser)\\n            } else {\\n              return graphcoolUser.id\\n            }\\n          })\\n        })\\n        .then(generateGraphcoolToken)\\n        .then(token => {\\n          return { data: { token } }\\n        })\\n    })\\n    .catch((error) => {\\n      console.log(error)\\n\\n      return { error: error.message }\\n    })\\n}\\n\",\"./schemas/github-authentication.graphql\":\"type AuthenticateGithubUserPayload {\\n  token: String!\\n}\\n\\nextend type Mutation {\\n  authenticateGithubUser(githubCode: String!): AuthenticateGithubUserPayload\\n}\\n\"}}]}"}})
      .reply(200, ["1f8b0800000000000003dc586d6fdb3610fe2b8c304032e0c80dba1585006f0b12afc8d0a5c1e2a01faa225124da662a532a4925cd5cfff71d5f2491b26c271b8600f3174be4bddf73c713575e9688c48b565e926517acb8c3a9906f65fb489325f622cf6ca2072216e88c128166ac58a29a6758b39ce219815d52d08f40f91bc9f1494105a620cb5bc514a1d85b165995631e7b11faa496e4a2542357622ff686f562aa59f5ba782c318f5038520fe19c25e5e26b1ec77456d1542ae4512c19e7a0b7ba3d4c2ab1005e9226724f6f21b44868966356bf229416196edf10e22c8d90b170a4458d24cda8576c78c70daf342a420cf322bfc7cc2cf2748197c9863cbdccb7886c1d2b315b12ce956be8d36758614521a6c517ac7d3decf71576ac18ce8889f5aa7632f6ba316ce28bde2981571c334496658e972096a3730800f04b01248bd0d9e901fa95f02b4abe5658ada60c270267c72242a7f030254b7c602543ca3b03c64bc1089d7778ab32ebe35d373e5898795a46943b801dae413ab9876d3486e47cad08c381afdc4e8b223fccc9ad3f081b2219b9988e46409a642616d2b74cca4f72ae318fe93d610595a141f70923c9ad8c704cb5c63427b0714d32d008359162ce43e0084fde9f4dcea7d767a75d4a8e4183e8a7be9c9cfc39996ab3740842fcad2c18e4648c6adca3004be3072643527801847931371b9a1f723743c141af4de8fb77d4b7a3f5d7925dd9fe458e138ea5d9f704f091403072705bbb054041506b487b77e00f8c0478a918452b84192b58847c13e563278b8816422a9b917905e18747c6a0b9e48fa1bf5682d68e4fcacd50758b6f226cd21b9689789eedb2be909005f6df596c922f4b6a8cb4e5b209871ace27b06e9135bec87cd7306dd35a9325250182d671780f7cae2a78747fe43708682033c742bba1ba090ab684e91d1642566c924a64e8c88461d80a6cc233c3225d04fe4288924723539b9092e50804113a2a64998eb49c6b25c71f363a115a62b128a043f8171f2ea7feb0595f402162c6238b1421df1c29875368593e302565999b3c8cee78412d01407d0c4a4bd14bd790ad5b8edb227b8cd0ef971fce43aefa15993d06b6faa6c2879b8b1aeece469d50a5a60655f3104a1005120068fc335240909605039740ae4982d60e097cb91a2a580e9c0089052b1e10c50f68223715dda03502e19c6387c14e7a4d6c6d9b1c2b75760a5b911b6e4194010fca36d76ea73d29d36d5d46138c1570f2e190613953042bc007e7c91c4e585f79841e1670ac21bbf5034ad56ca261ed6f847abdbd0600204545c5a94c42306fcb627f5148369db41d4571531705546568154605a7e22f7638c73fac2cedeb9b36651a04305d9460089601ad9f3b68d98e97dd88d987995ed4ecc58d831c5bd66641ec82cc3ed03c1736739340d3c8651e54127ddb9c9db0a91bad1a948276c8d98398864fe9ecc78c04899c543017c14d630dbcb3472724eda416b843561cc75e0d24b916926c2dd7ba0927597f46ccd38ddb806cecb94da841e2131a514dfbe46664336ce6bb51dd06e3a55bd2bc37d54f69477a867e2eb44e24d7bfc3d6b2127a847192a0ad6941e660c741dc36c0591cff67ec7523155acebd1008d37e503ced50a498d9383403e2dcc6e559d686da58d6ce9eb504391f2be62eefb01ea1e5abff320780325146488fa2bb1a7feda03331072e42ad63dbb5bccb6c8d1acea461b9624b544da0130a47a81d59bb5918214e33d990a36ba843341e235ae579b7622dbd3d8dca56dd61eb9d1bfa80d3ad1db7829cdcf4bc18877bd1bb492736336599b452d3007c736874800beb3eb5dda236c0fc0765dd7c5baabfd080d6ed1e0a8f3bae449e76a9d4def4589faf56f3ba481ef322c98c03cafdfadae6a03622a6f0958de1db5ec9f9c33d3d925eb141fb71db881b44bb6d687c36415045f91916bda1071feb09f72209d4a147322ff2d2bbb73f7ea16feffe7af59abd7975f4e6b5f8692170956640ad430344fbafb868df0d17dd72c145f7de6fd12dd75bca0986e71037306b7275fd717239bd3ef2d6f0fb1b0000ffff","03005c032ffd94150000"], [ 'Content-Type',
        'application/json',
        'Transfer-Encoding',
        'chunked',
        'Connection',
        'close',
        'Vary',
        'Accept-Encoding',
        'Date',
        'Thu, 28 Sep 2017 14:45:19 GMT',
        'Request-Id',
        'eu-west-1:system:cj84kn8ja03r401631cvaohvr',
        'Server',
        'akka-http/10.0.8',
        'Content-Encoding',
        'gzip',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 a6cbd015f961a0db7be2b1cd285e26d9.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        'EIvezbQ-uL7tQ_fTKWveOCkquNihenr2dvf2UkE066M7BMCCHaQGWw==' ]);

    const config = new Config()
    fs.copySync(path.join(__dirname, 'test-project/'), config.definitionDir)
    const result = await Init.mock(
      {mockConfig: config},
      '--name',
      'Project with Init from Project',
    )
    expect(result.out.stdout.output).toMatchSnapshot()
  }, 30000)
})
