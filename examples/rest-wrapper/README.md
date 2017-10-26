# REST API Wrapper


## Overview

This directory contains the service definition and file structure for a simple Graphcool service that **wraps a REST API** using [resolver](https://blog.graph.cool/extend-your-graphcool-api-with-resolvers-ca0f0270bca7) functions. Read the [last section](#whats-in-this-example) of this README to learn how the different components fit together.

The example is based on the free [Dog API](https://dog.ceo/dog-api/) which allows to retrieve images of dogs that belong to certain breeds and subbreeds.

```
.
├── README.md
├── graphcool.yml
├── src
│   ├── allBreeds.graphql
│   ├── allBreeds.js
│   ├── allSubBreeds.graphql
│   ├── allSubBreeds.js
│   ├── breedImages.graphql
│   ├── breedImages.js
│   ├── randomBreedImage.graphql
│   ├── randomBreedImage.js
│   ├── randomDogImage.graphql
│   ├── randomDogImage.js
│   ├── randomSubBreedImage.graphql
│   ├── randomSubBreedImage.js
│   ├── subBreedImages.graphql
│   └── subBreedImages.js
└── types.graphql
```

> Read more about [service configuration](https://graph.cool/docs/reference/project-configuration/overview-opheidaix3) in the docs.

## Get started

### 1. Download the example

Clone the full [graphcool](https://github.com/graphcool/graphcool) repository and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/rest-wrapper
cd rest-wrapper
```

Next, you need to create your GraphQL server using the [Graphcool CLI](https://graph.cool/docs/reference/graphcool-cli/overview-zboghez5go).

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the CLI first:

```sh
npm install -g graphcool
```

### 3. Create the GraphQL server

The next step will be to [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory. 

To deploy the service and actually create your GraphQL server, invoke the following command:

```sh
graphcool deploy
```


When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`. 

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.


## Testing the service


The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

You can open a Playground with the following command:

```sh
graphcool playground
```

Inside the Playground, you can send queries for all the resolver functions that are defined inside [`graphcool.yml`](./graphcool.yml):

- `allBreeds`
- `allSubBreeds`
- `randomDogImage`
- `randomBreedImage`
- `randomSubBreedImage`
- `breedImages`
- `subBreedImages`

Here are are some sample queries you can send:

##### Get all breeds and their subbreeds

```graphql
{
  allBreeds {
    name
    subBreeds
  }
}
```

##### Get all subbreeds of the `hound` breed

```graphql
{
  allSubBreeds(breedName: "hound") {
    name
  }
}
```

##### Get a random dog image

```graphql
{
  randomDogImage {
    url
  }
}
```

##### Get a list of of the `afghan` breed (subbreed of `hound`)

```graphql
{
  subBreedImages(breedName: "hound", subBreedName: "afghan") {
    url
  }
}
```

## What's in this example?

This example demonstrates how you can use Graphcool's [resolver functions](https://graph.cool/docs/reference/functions/resolvers-su6wu3yoo2) to wrap an existing REST API (a [Dog API](https://dog.ceo/dog-api/) to retrieve images of certain breeds and their subbreeds).

Each resolver targets one dedicated endpoint from the API and effectively acts as a proxy to provide a GraphQL API for the existing REST endpoint.

For example, the `allBreeds` resolver provides the following API defined in [`allBreeds.graphql`](./src/allBreeds.graphql):

```graphql
type AllBreedsPayload {
  name: String!
  subBreeds: [String!]!
}

extend type Query {
  allBreeds: [AllBreedsPayload!]!
}
``` 

It's implementation in [`allBreeds.js`](./src/allBreeds.js) looks as follows:

```js
require('isomorphic-fetch')

const url = 'https://dog.ceo/api/breeds/list/all'

module.exports = () => {
  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const breedsListData = responseData.message
      const allBreeds = []
      Object.keys(breedsListData).map(breedName => {
        const breed = {
          name: breedName,
          subBreeds: breedsListData[breedName]
        }
        allBreeds.push(breed)
      })
      return { data: allBreeds }
    })
}
```

All that's happening in there is sending an HTTP request to the specified endpoint and convert the response into the format that's defined in the extension of the `Query` type above.










