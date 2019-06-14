import { Env } from '@prisma/cli'
import { Introspect } from './Introspect'

async function run() {
  const env = await Env.load(process.env, process.cwd())

  await Introspect.new(env as any).parse(process.argv.slice(2))
}

run()
