import { grabRelease, linux, checkEnvs } from './release-lib'

async function main() {
  try {
    checkEnvs()
    const version = await grabRelease()
    await linux(version)
  } catch (e) {
    throw new Error(e)
  }
}

main()
