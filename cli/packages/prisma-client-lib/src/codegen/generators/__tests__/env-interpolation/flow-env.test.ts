import { FlowGenerator } from '../../flow-client'
import { test } from 'ava'

test('flow env interpolation - plain', t => {
  const result = FlowGenerator.replaceEnv(`http://localhost:4466/test/dev`)
  t.snapshot(result)
})

test('flow env interpolation - environment one', t => {
  const result = FlowGenerator.replaceEnv('${env:PRISMA_ENDPOINT}')
  t.snapshot(result)
})

test('flow env interpolation - environment multiple', t => {
  const result = FlowGenerator.replaceEnv(
    'http://localhost:4466/${env:PRISMA_SERVICE}/${env:PRISMA_STAGE}',
  )
  t.snapshot(result)
})
