import { PostgresConnector } from 'prisma-db-introspection'
import { generateClient } from '@prisma/photon'
import { isdlToDatamodel2 } from '@prisma/lift'
import { ISDL } from 'prisma-datamodel'
import { join, dirname } from 'path'
import { writeFile } from 'mz/fs'
import mkdir from 'make-dir'
import { Client } from 'pg'
import assert from 'assert'
import pkgup from 'pkg-up'
import exec from 'execa'
import del from 'del'

const db = new Client({
  connectionString: 'postgres://localhost:5432/prisma-dev',
})

const pkg = pkgup.sync() || __dirname
const tmp = join(dirname(pkg), 'tmp')

beforeAll(async () => {
  // TODO: hangs if postgres is down
  await db.connect()
})

beforeEach(async () => {
  await del(tmp)
  await mkdir(tmp)
})

afterAll(async () => {
  await db.end()
})

tests().map(t => {
  if (t.todo) {
    test.todo(t.name)
    return
  }

  test(
    t.name,
    // async () => {
    //   await db.query(t.after)
    //   await db.query(t.before)
    //   const bin = join(process.cwd(), 'build', 'index.js')

    //   // $ prisma introspect
    //   const { stdout } = await exec(
    //     bin,
    //     [
    //       'introspect',
    //       '--pg-host',
    //       'localhost',
    //       '--pg-db',
    //       'prisma-dev',
    //       '--pg-user',
    //       'm',
    //       '--pg-schema',
    //       'public',
    //       '--pg-password',
    //       '',
    //       '--sdl',
    //     ],
    //     { cwd: tmp },
    //   )

    //   // write a prisma file
    //   await writeFile(join(tmp, 'datamodel.prisma'), stdout)

    //   // $ prisma generate
    //   await exec(bin, ['generate'], { cwd: tmp })

    //   const { Photon } = await import(join(tmp, 'node_modules', '@generated', 'photon', 'index.js'))
    //   const client = new Photon()
    //   try {
    //     const result = await t.do(client)
    //     await db.query(t.after)
    //     assert.deepEqual(result, t.expect)
    //   } catch (err) {
    //     throw err
    //   } finally {
    //     await client.disconnect()
    //   }
    // },
    async () => {
      await db.query(t.after)
      await db.query(t.before)
      const isdl = await inspect(db, 'public')

      // $ prisma introspect
      const bin = join(process.cwd(), 'build', 'index.js')
      const { stdout } = await exec(
        bin,
        [
          'introspect',
          '--pg-host',
          'localhost',
          '--pg-db',
          'prisma-dev',
          '--pg-user',
          'm',
          '--pg-schema',
          'public',
          '--pg-password',
          '',
          '--sdl',
        ],
        { cwd: tmp },
      )
      // console.log(stdout)

      await generate(isdl)
      const { default: Photon } = await import(join(tmp, 'index.js'))
      const client = new Photon()
      try {
        const result = await t.do(client)
        await db.query(t.after)
        assert.deepEqual(result, t.expect)
      } catch (err) {
        throw err
      } finally {
        await client.disconnect()
      }
    },
    30000,
  )
})

async function inspect(client: Client, schema: string): Promise<ISDL> {
  const connector = new PostgresConnector(client)
  const result = await connector.introspect(schema)
  return result.getNormalizedDatamodel()
}

// async function migrate() {}

async function generate(isdl: ISDL) {
  const datamodel = await isdlToDatamodel2(isdl, [
    {
      name: 'pg',
      connectorType: 'postgres',
      url: `postgres://m@localhost:5432/prisma-dev?schema=public`,
      config: {},
    },
  ])
  await generateClient({
    datamodel: datamodel,
    cwd: tmp,
    outputDir: tmp,
    transpile: true,
    runtimePath: '../runtime',
  })
}

function tests() {
  return [
    {
      name: 'teams.findOne',
      before: `
        create table if not exists teams (
          id int primary key not null,
          name text not null unique
        );
        insert into teams (id, name) values (1, 'a');
        insert into teams (id, name) values (2, 'b');
      `,
      after: `
        drop table if exists teams cascade;
      `,
      do: async client => {
        return client.teams.findOne({ where: { id: 2 } })
      },
      expect: {
        id: 2,
        name: 'b',
      },
    },
    {
      name: `teams.create`,
      before: `
        create table if not exists teams (
          id serial primary key not null,
          name text not null unique
        );
      `,
      after: `
        drop table if exists teams cascade;
      `,
      do: async client => {
        return client.teams.create({ data: { name: 'c' } })
      },
      expect: {
        id: 1,
        name: 'c',
      },
    },
    {
      name: `teams.update`,
      before: `
        create table if not exists teams (
          id serial primary key not null,
          name text not null unique
        );
        insert into teams ("name") values ('c');
      `,
      after: `
        drop table if exists teams cascade;
      `,
      do: async client => {
        return client.teams.update({
          where: { name: 'c' },
          data: { name: 'd' },
        })
      },
      expect: {
        id: 1,
        name: 'd',
      },
    },
    {
      name: `users.findOne({ where: { email })`,
      before: `
        create table if not exists users (
          id serial primary key not null,
          email text not null unique
        );
        insert into users ("email") values ('ada@prisma.io');
      `,
      after: `
        drop table if exists users cascade;
      `,
      do: async client => {
        return client.users.findOne({ where: { email: 'ada@prisma.io' } })
      },
      expect: {
        id: 1,
        email: 'ada@prisma.io',
      },
    },
    {
      name: `users({ email })`,
      before: `
        create table if not exists users (
          id serial primary key not null,
          email text not null unique
        );
        insert into users ("email") values ('ada@prisma.io');
      `,
      after: `
        drop table if exists users cascade;
      `,
      do: async client => {
        return client.users({ where: { email: 'ada@prisma.io' } })
      },
      expect: [
        {
          id: 1,
          email: 'ada@prisma.io',
        },
      ],
    },

    {
      name: `users()`,
      before: `
        create table if not exists users (
          id serial primary key not null,
          email text not null unique
        );
        insert into users ("email") values ('ada@prisma.io');
        insert into users ("email") values ('ema@prisma.io');
      `,
      after: `
        drop table if exists users cascade;
      `,
      do: async client => {
        return client.users()
      },
      expect: [
        {
          id: 1,
          email: 'ada@prisma.io',
        },
        {
          id: 2,
          email: 'ema@prisma.io',
        },
      ],
    },
    {
      todo: true,
      name: `user.posts()`,
      before: `
        create table if not exists users (
          id serial primary key not null,
          email text not null unique
        );
        create table if not exists posts (
          id serial primary key not null,
          user_id int not null references users (id) on update cascade,
          title text not null
        );
        insert into users ("email") values ('ada@prisma.io');
        insert into users ("email") values ('ema@prisma.io');
        insert into posts ("user_id", "title") values (1, 'A');
        insert into posts ("user_id", "title") values (1, 'B');
        insert into posts ("user_id", "title") values (2, 'C');
      `,
      after: `
        drop table if exists posts cascade;
        drop table if exists users cascade;
      `,
      do: async client => {
        return client.users.findOne({ where: { email: 'ada@prisma.io' } }).posts()
      },
      expect: [
        {
          id: 1,
          email: 'ada@prisma.io',
        },
        {
          id: 2,
          email: 'ema@prisma.io',
        },
      ],
    },
  ]
}
