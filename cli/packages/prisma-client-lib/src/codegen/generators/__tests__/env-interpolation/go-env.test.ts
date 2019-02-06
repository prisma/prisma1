import { GoGenerator } from '../../go-client'
import { test } from 'ava'

test('go env interpolation - empty', t => {
  const result = GoGenerator.replaceEnv(``)
  t.snapshot(result)
})

test('go env interpolation - null', t => {
  const result = GoGenerator.replaceEnv(null as any)
  t.snapshot(result)
})

test('go env interpolation - plain', t => {
  const result = GoGenerator.replaceEnv(`http://localhost:4466/test/dev`)
  t.snapshot(result)
})

test('go env interpolation - environment one', t => {
  const result = GoGenerator.replaceEnv('${env:PRISMA_ENDPOINT}')
  t.snapshot(result)
})

test('go env interpolation - environment multiple', t => {
  const result = GoGenerator.replaceEnv(
    'http://localhost:4466/${env:PRISMA_SERVICE}/${env:PRISMA_STAGE}',
  )
  t.snapshot(result)
})
