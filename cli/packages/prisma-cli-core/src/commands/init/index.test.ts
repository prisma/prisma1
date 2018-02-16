import * as nock from "nock";
import * as fs from "fs-extra";
import * as path from "path";
import { Config } from "prisma-cli-engine";
import { getTmpDir } from "../../test/getTmpDir";
import Init from "./";
import { tmpdir } from "os";

afterAll(() => {
  nock.cleanAll();
});

describe("init", () => {
  test("test project", async () => {
    const tmpDir = getTmpDir();
    const result = await Init.mock(tmpDir, "-m");

    expect(result.out.stdout.output).toContain("Created 3 new files");

    fs.readdir(tmpDir, (err, files) => {
      files.map(file => expect(file).toMatchSnapshot());
    });
  });
});

