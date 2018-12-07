const spawnAsync = require('@expo/spawn-async')
const { request } = require('graphql-request')

async function run() {
  const before = Date.now()
  const resultPromise = spawnAsync('./prisma-native')

  await retryUntilWorks()
  const after = Date.now()

  console.log(`Needed ${after - before}ms for initial request`)
  let times = []
  for (var i = 0; i < 5000; i++) {
    times.push(await retryUntilWorks(true))
    if (i % 10 === 0 && i > 0) {
      const avg = times.reduce((acc, curr) => acc + curr, 0) / times.length
      console.log(`Average after ${i} requests: ${avg}ms`)
    }
  }
}

run().catch(console.error)

async function retryUntilWorks(log = false) {
  const before2 = Date.now()
      const result = await request(
        `http://localhost:4467`,
        `
    {
      cats {
        id
      }
    }
    `,
      )
      const after2 = Date.now()
      if (log) {
        // console.log(`Needed ${after2 - before2}ms for warm request`)
      }
      return after2 - before2
}
