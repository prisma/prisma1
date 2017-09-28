#!/usr/bin/env node

import * as path from 'path'
import * as semver from 'semver'
import * as fs from 'fs-extra'
import {run} from 'graphcool-cli-engine'

const root = path.join(__dirname, '..')

const pjson = fs.readJsonSync(path.join(root, 'package.json'))

const nodeVersion = process.version.split('v')[1]
if (!semver.satisfies(nodeVersion, pjson.engines.node)) {
  process.stderr.write(`WARNING\nWARNING Node version must be ${pjson.engines.node} to use the Graphcool CLI\nWARNING\n`)
}

run({config: {root, mock: false}})
