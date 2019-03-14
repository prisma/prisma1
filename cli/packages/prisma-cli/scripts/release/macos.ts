import { grabRelease, brew, checkEnvs } from './release-lib'

async function main() {
  try {
    checkEnvs()
    const version = await grabRelease()
    await brew(version)
  } catch (e) {
    console.error(e)
    process.exit(1)
  }
}

main()
