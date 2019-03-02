import { JavascriptGenerator } from '../../javascript-client'
import { test } from 'ava'

test('javascript env interpolation - plain', t => {
  const result = JavascriptGenerator.replaceEnv(
    `http://localhost:4466/test/dev`,
  )
  t.snapshot(result)
})

test('javascript env interpolation - environment one', t => {
  const result = JavascriptGenerator.replaceEnv('${env:PRISMA_ENDPOINT}')
  t.snapshot(result)
})

test('javascript env interpolation - environment multiple', t => {
  const result = JavascriptGenerator.replaceEnv(
    'http://localhost:4466/${env:PRISMA_SERVICE}/${env:PRISMA_STAGE}',
  )
  t.snapshot(result)
})
