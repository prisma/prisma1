# crud-api

[Demo](https://api.graph.cool/simple/v1/cj86c7ph908850131tcpyy572)


## Add initial data

Run the following mutation to add some initial data. (Feel free to change this as you want.)

```graphql
mutation {
  elon: createUser(
    firstName: "Elon"
    lastName: "Musk"
    posts: [{
      title: "Earth Travel"
      description: "Fly to most places on Earth in under 30 mins and anywhere in under 60. Cost per seat should be about the same as full fare economy in an aircraft. Forgot to mention that."
    }, {
      title: "Mars City"
      description: "Opposite of Earth. Dawn and dusk sky are blue on Mars and day sky is red."
    }]
  ) {
    id
  }
  
  tim: createUser(
    firstName: "Tim"
    lastName: "Cook"
    posts: [{
      title: "State Affairs"
      description: "A pleasure to host Secretary of Defense James Mattis at Amazon HQ in Seattle today"
    }]
  ) {
    id
  }
}
```
