import testSchema from '../common'
import * as fs from 'fs'
import * as path from 'path'

describe('Introspector', () => {
  test.skip(
    'allsquare',
    async () => {
      await testSchema(
        fs.readFileSync(path.join(__dirname, 'allsquare.sql'), 'utf-8'),
        'allsquare',
      )
    },
    60000,
  )
})
