import { getReleaseFromGithubAPI, linux, checkEnvs, updateFiles } from './release-lib'

async function main() {
  try {
    checkEnvs()
    const version = await getReleaseFromGithubAPI()
    // Files are updated from binary-X commands which in invoked by the following function using spawn sync
    await linux(version)
  } catch (e) {
    throw new Error(e)
  }
}

main()
