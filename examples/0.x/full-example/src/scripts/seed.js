const { GraphQLClient } = require('graphql-request')

const ENDPOINT = '__ENDPOINT__'
const ROOT_TOKEN = '__ROOT_TOKEN__'

const client = new GraphQLClient(ENDPOINT, {
  headers: {
    Authorization: `Bearer ${ROOT_TOKEN}`,
  },
})

const mutation = `mutation {
  createProduct(
    description: "The new shiny iPhone"
    name: "iPhone X"
    price: 1200
    imageUrl: "https://cdn.vox-cdn.com/uploads/chorus_image/image/56645405/iphone_x_gallery1_2017.0.jpeg"
  ) {
    id
  }
}`

client.request(mutation)
  .then(data => console.log(data))
  .catch(err => {
    console.log(err)
  })
