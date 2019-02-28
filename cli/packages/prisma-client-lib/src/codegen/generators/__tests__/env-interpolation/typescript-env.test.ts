import { TypescriptGenerator } from '../../typescript-client'
import { test } from 'ava'

test('typescript env interpolation - plain', t => {
  const result = TypescriptGenerator.replaceEnv(
    `http://localhost:4466/test/dev`,
  )
  t.snapshot(result)
})

test('typescript env interpolation - environment one', t => {
  const result = TypescriptGenerator.replaceEnv('${env:PRISMA_ENDPOINT}')
  t.snapshot(result)
})

test('typescript env interpolation - environment multiple', t => {
  const result = TypescriptGenerator.replaceEnv(
    'http://localhost:4466/${env:PRISMA_SERVICE}/${env:PRISMA_STAGE}',
  )
  t.snapshot(result)
})
