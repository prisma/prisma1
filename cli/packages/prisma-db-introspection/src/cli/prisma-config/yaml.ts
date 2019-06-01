import * as Ajv from 'ajv'
import * as fs from 'fs'
import * as yaml from 'js-yaml'
import stringify from 'json-stable-stringify'
import { PrismaDefinition } from 'prisma-json-schema'
import { Args, IOutput, Output, Variables } from 'prisma-yml'
import schema = require('prisma-json-schema/dist/schema.json')
const debug = require('debug')('yaml')

const ajv = new Ajv()

ajv.addMetaSchema(require('ajv/lib/refs/json-schema-draft-06.json'))

const validate = ajv.compile(schema)
// this is used by the playground, which accepts additional properties
const validateGraceful = ajv.compile({ ...schema, additionalProperties: true })

const cache: any = {}

export async function readDefinition(
  filePath: string,
  args: Args,
  out: IOutput = new Output(),
  envVars?: any,
  graceful?: boolean,
): Promise<{ definition: PrismaDefinition; rawJson: any }> {
  if (!fs.existsSync(filePath)) {
    throw new Error(`${filePath} could not be found.`)
  }
  const file = fs.readFileSync(filePath, 'utf-8')
  const json = yaml.safeLoad(file) as PrismaDefinition
  // we need this copy because populateJson runs inplace
  const jsonCopy = { ...json }

  const vars = new Variables(filePath, args, out, envVars)
  const populatedJson = await vars.populateJson(json)
  if (populatedJson.custom) {
    delete populatedJson.custom
  }
  const valid = graceful
    ? validateGraceful(populatedJson)
    : validate(populatedJson)
  // TODO activate as soon as the backend sends valid yaml
  if (!valid) {
    let errorMessage =
      `Invalid prisma.yml file` +
      '\n' +
      printErrors(graceful ? validateGraceful.errors! : validate.errors!)
    throw new Error(errorMessage)
  }

  cache[file] = populatedJson
  return {
    definition: populatedJson,
    rawJson: jsonCopy,
  }
}

function printErrors(errors: any[], name = 'prisma.yml') {
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

const betterMessagesByParams: Record<string, string> = {
  // this is not up-to-date, stages are in again!
  // https://github.com/prisma/framework/issues/1461
  '{"additionalProperty":"stages"}':
    'prisma.yml should NOT have a "stages" property anymore. Stages are now just provided as CLI args.\nRead more here: https://goo.gl/SUD5i5',
  '{"additionalProperty":"types"}':
    'prisma.yml should NOT have a "types" property anymore. It has been renamed to "datamodel"',
}
