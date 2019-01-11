import { spawnSync } from 'child_process';
import * as AWS from 'aws-sdk';
import axios from 'axios';
import * as os from 'os';
import * as fs from 'fs-extra';
import { gitCommitPush } from 'git-commit-push-via-github-api';
const token = process.env.GITHUB_TOKEN;

AWS.config.update({
  accessKeyId: process.env.AWS_ACCESS_KEY_ID,
  secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY
});

axios.defaults.headers.common.Authorization = `Bearer ${token}`;

async function main() {
  const tData = await axios.get(
    'https://api.github.com/repos/prisma/prisma/releases'
  );
  const stableReleaseVersion = tData.data.filter(
    node => !node.tag_name.includes('alpha') && !node.tag_name.includes('beta')
  )[0].tag_name;
  console.log(`Version to publish: ${stableReleaseVersion}`);

  console.log('Creating binary');
  const buildResponse = spawnSync('npm', ['run', 'make-binary']);
  console.log(`Created binary.... ${buildResponse.stdout.toString()}`);

  const tarFileName = `prisma-${stableReleaseVersion}.tar.gz`;
  const tarResponse = spawnSync('tar', ['-cvzf', tarFileName, 'prisma']);
  console.log('made tar', tarResponse.stdout.toString());
  const shaResponse = spawnSync('shasum', ['-a', '256', tarFileName]);
  const shaValue = shaResponse.stdout
    .toString()
    .split(' ')[0]
    .trim();
  console.log(`shasum -a 256 ${tarFileName} => ${shaValue}`);

  console.log('Uploading tar to S3....this may take a while');

  const fileData = await fs.readFile(tarFileName);
  const s3 = new AWS.S3({ params: { timeout: 6000000 } });

  const s3Resp = await s3
    .upload({
      Bucket: 'homebrew-prisma',
      Key: tarFileName,
      Body: fileData,
      ACL: 'public-read'
    })
    .promise();
  console.log('Tar uploaded at location', s3Resp.Location);

  const uploadedBinaryURL = s3Resp.Location;
  console.log(`uploaded binary url ${uploadedBinaryURL}`);
  let homebrewDefinition = ``;
  const homeBrewTmp = `
class Prisma < Formula
  desc "Prisma turns your database into a realtime GraphQL API"
  homepage "https://github.com/prisma/prisma"
  url "https://s3-eu-west-1.amazonaws.com/homebrew-prisma/prisma-1.22.0.patch.1.tar.gz"
  sha256 "052cc310ab3eae8277e4d6fbf4848bc5c518af8e5165217a384bc26df82e63b9"
  version "1.22.0.patch.1"

  bottle :unneeded

  def install
    bin.install "prisma"
  end
end
  `;
  homeBrewTmp.split(/\r?\n/).forEach(line => {
    if (line.includes('version')) {
      homebrewDefinition += `  version "${stableReleaseVersion}"${os.EOL}`;
    } else if (line.includes('url')) {
      homebrewDefinition += `  url "${uploadedBinaryURL}"${os.EOL}`;
    } else if (line.includes('sha256')) {
      homebrewDefinition += `  sha256 "${shaValue}"${os.EOL}`;
    } else {
      homebrewDefinition += `${line}${os.EOL}`;
    }
  });

  await gitCommitPush({
    owner: 'prisma',
    repo: 'homebrew-prisma',
    token,
    files: [{ path: 'prisma.rb', content: homebrewDefinition }],
    fullyQualifiedRef: 'heads/automated-pr-branch',
    commitMessage: `bump version to ${stableReleaseVersion}`
  });

  const pullRes = await axios.post(
    'https://api.github.com/repos/prisma/homebrew-prisma/pulls',
    {
      title: `Automated PR for version ${stableReleaseVersion}`,
      head: 'automated-pr-branch',
      base: 'master',
      body: ' Automated PR generated via script',
      maintainer_can_modify: true
    }
  );
  console.log(
    `Pull Request created at ${
      pullRes.data.html_url
    }. Merge this to complete the release`
  );
}
main();
