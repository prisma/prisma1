import * as faker from 'faker'
import * as fs from 'fs-extra'
import * as path from 'path'

const defaultN = 50

const start = 400

function makeNodes(n: number = defaultN) {
  const values: any[] = []

  for (let i = 0; i < n; i++) {
    values.push({
      _typeName: 'Post',
      id: String(start + i),
      title: faker.lorem.sentence(),
      description: faker.lorem.paragraphs(),
      state: 'Published',
    })
  }
  for (let i = 0; i < n; i++) {
    values.push({
      _typeName: 'Comment',
      id: String(start + n + i),
      text: faker.lorem.sentence(),
    })
  }

  return {
    valueType: 'nodes',
    values,
  }
}

function makeLists(n: number = defaultN) {
  const values: any[] = []

  for (let i = 0; i < n; i++) {
    values.push({
      _typeName: 'Post',
      id: String(start + i),
      tags: ['a', 'b', 'c'],
    })
  }

  return {
    valueType: 'lists',
    values,
  }
}

function makeRelations(n: number = defaultN) {
  const values: any[] = []

  for (let i = 0; i < n; i++) {
    values.push([
      {
        _typeName: 'Post',
        id: String(start + i),
        fieldName: 'comments',
      },
      {
        _typeName: 'Comment',
        id: String(start + n + i),
        fieldName: 'post',
      },
    ])
  }

  return {
    valueType: 'relations',
    values,
  }
}

const nodes = makeNodes()
const lists = makeLists()
const relations = makeRelations()

fs.writeFileSync(
  path.join(__dirname, '.import/nodes/', '000001.json'),
  JSON.stringify(nodes, null, 2),
)
fs.writeFileSync(
  path.join(__dirname, '.import/lists/', '000001.json'),
  JSON.stringify(lists, null, 2),
)
fs.writeFileSync(
  path.join(__dirname, '.import/relations/', '000001.json'),
  JSON.stringify(relations, null, 2),
)
