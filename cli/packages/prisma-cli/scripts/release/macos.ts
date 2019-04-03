import { getReleaseFromGithubAPI, brew, checkEnvs, updateFiles } from './release-lib'

async function main() {
  try {
    checkEnvs()
    const version = await getReleaseFromGithubAPI()
    // Files are updated from binary-X commands which in invoked by the following function using spawn sync
    await brew(version)
  } catch (e) {
    console.error(e)
    process.exit(1)
  }
}

main()
