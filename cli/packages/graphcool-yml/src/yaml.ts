import * as Ajv from 'ajv'
import * as yaml from 'js-yaml'
import * as fs from 'fs-extra'
import schema = require('graphcool-json-schema/dist/schema.json')
import { GraphcoolDefinition } from 'graphcool-json-schema'
import { Variables } from './Variables'
import { Args } from './types/common'
import { Output, IOutput } from './Output'
const debug = require('debug')('yaml')
import * as stringify from 'json-stable-stringify'
import chalk from 'chalk'

const ajv = new Ajv()

const validate = ajv.compile(schema)

const cache = {}

export async function readDefinition(
  filePath: string,
  args: Args,
  out: IOutput = new Output(),
  envVars?: any,
): Promise<{ definition: GraphcoolDefinition; rawJson: any }> {
  if (!fs.pathExistsSync(filePath)) {
    throw new Error(`${filePath} could not be found.`)
  }
  const file = fs.readFileSync(filePath, 'utf-8')
  const json = yaml.safeLoad(file) as GraphcoolDefinition
  // we need this copy because populateJson runs inplace
  const jsonCopy = { ...json }

  const vars = new Variables(filePath, args, out, envVars)
  const populatedJson = await vars.populateJson(json)
  if (populatedJson.custom) {
    delete populatedJson.custom
  }
  const valid = validate(populatedJson)
  // TODO activate as soon as the backend sends valid yaml
  if (!valid) {
    debugger
    let errorMessage =
      `Invalid graphcool.yml file` + '\n' + printErrors(validate.errors!)
    throw new Error(errorMessage)
  }

  cache[file] = populatedJson
  return {
    definition: populatedJson,
    rawJson: jsonCopy,
  }
}

function printErrors(errors, name = 'graphcool.yml') {
  return errors
    .map(e => {
      const paramsKey = stringify(e.params)
      if (betterMessagesByParams[paramsKey]) {
        return betterMessagesByParams[paramsKey]
      }
      const params = Object.keys(e.params)
        .map(key => `${key}: ${e.params[key]}`)
        .join(', ')
      debug(stringify(e.params))
      return `${name}${e.dataPath} ${e.message}. ${params}`
    })
    .join('\n')
}

const betterMessagesByParams = {
  // this is not up-to-date, stages are in again!
  // https://github.com/graphcool/framework/issues/1461
  '{"additionalProperty":"stages"}':
    'graphcool.yml should NOT have a "stages" property anymore. Stages are now just provided as CLI args.\nRead more here: https://goo.gl/SUD5i5',
}
