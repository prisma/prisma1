const faker = require('faker')
const fs = require('fs')

const items: any[] = []

for (let i = 0; i < 5; i++) {
  items.push({
    _typeName: 'Post',
    id: '' + i,
    title: faker.lorem.sentence(),
    description: faker.lorem.paragraphs(),
  })
}

const json = JSON.stringify(items)
fs.writeFileSync('data.json', json)
