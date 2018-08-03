import * as fs from 'fs-extra'
import * as path from 'path'
import { Importer } from './Importer';
import { Environment } from 'prisma-yml'
import { Client, Config, Output } from 'prisma-cli-engine';
import { getTmpDir } from '../../../../prisma-yml/src/test/getTmpDir';
import * as cuid from 'scuid'

const tmpDir = getTmpDir()

function initImporter() {
    const config = new Config()
    const output = new Output(config)
    const env = new Environment(getTmpDir(), output)
    const client = new Client(config, env, output)
    const types = `
    type User {
      id: ID! @unique
      name: String!
    }`
    return new Importer(tmpDir, types, client, output, config);
}

describe('Importer', () => {
    const importer = initImporter()
    const nodesDir = path.join(tmpDir, 'nodes')
    fs.mkdirpSync(nodesDir)
    fs.copyFileSync(
        path.join(__dirname, 'boilerplate', '1.json'),
        `${nodesDir}/${cuid()}.json`
    )

    it('throws when ids are missing', async () => {
        expect(async () => importer.upload('serviceName', 'stage')).toThrow()
    });
})
