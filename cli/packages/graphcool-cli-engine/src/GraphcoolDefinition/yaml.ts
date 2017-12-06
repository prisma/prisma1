import * as Ajv from 'ajv'
import * as yaml from 'js-yaml'
import * as fs from 'fs-extra'
import schema = require('graphcool-json-schema/dist/schema.json')
import chalk from 'chalk'
import { GraphcoolDefinition } from 'graphcool-json-schema'
import { Output } from '../Output/index'
import Variables from './Variables'
import { Args } from '../types/common'
const debug = require('debug')('yaml')

const ajv = new Ajv()

const validate = ajv.compile(schema)

const cache = {}

export async function readDefinition(
  filePath: string,
  out: Output,
  args: Args,
): Promise<GraphcoolDefinition> {
  if (!fs.pathExistsSync(filePath)) {
    throw new Error(`${filePath} could not be found.`)
  }
  const file = fs.readFileSync(filePath, 'utf-8')
  const json = yaml.safeLoad(file) as GraphcoolDefinition

  const vars = new Variables(out, filePath, args)
  const populatedJson = await vars.populateJson(json)
  if (populatedJson.custom) {
    delete populatedJson.custom
  }
  const valid = validate(populatedJson)
  // TODO activate as soon as the backend sends valid yaml
  if (!valid) {
    out.log(
      out.getErrorPrefix(filePath) +
        chalk.bold('Errors while validating graphcool.yml:\n'),
    )
    out.error(
      chalk.red(
        ajv
          .errorsText(validate.errors)
          .split(', ')
          .map(l => `  ${l}`)
          .join('\n'),
      ),
    )
    out.exit(1)
  }

  cache[file] = populatedJson
  return populatedJson
}
