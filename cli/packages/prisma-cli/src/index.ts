#!/usr/bin/env node

import * as path from 'path'
import * as semver from 'semver'
import * as fs from 'fs-extra'
import {run} from 'prisma-cli-engine'
// import 'require-onct'

const root = path.join(__dirname, '..')

const pjson = fs.readJsonSync(path.join(root, 'package.json'))

const nodeVersion = process.version.split('v')[1]
if (!semver.satisfies(nodeVersion, pjson.engines.node)) {
  process.stderr.write(`WARNING\nWARNING Node version must be ${pjson.engines.node} to use the Prisma CLI\nWARNING\n`)
}

if (nodeVersion === '8.0.0' || nodeVersion === '8.1.0') {
  process.stderr.write(`WARNING\nWARNING Node version ${nodeVersion} is not supported. Please use at least 8.1.2 \nWARNING\n`)
}

run({config: {root, mock: false}})
