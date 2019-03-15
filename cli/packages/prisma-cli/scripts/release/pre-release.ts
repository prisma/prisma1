import { getReleaseFromGithubAPI, updateFiles } from './release-lib'

async function main() {
    const version = await getReleaseFromGithubAPI()
    updateFiles(version)
    console.log(`Version: ${version}`)
}

main()