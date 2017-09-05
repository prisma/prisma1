---
alias: tijghei9go
description: Relay introduces new concepts on top of GraphQL. Learn more about terms like connections, edges and nodes in Relay and see a pagination example.
preview: movies-actors.png
ordered: true
---

# Apollo is awesome
!InfoBox[This is a Info Box!](type="info"){type="info2"}

The terminology of Relay can be quite overwhelming in the beginning. Relay introduces a handful of new concepts on top of GraphQL, mainly in order to manage relationships between models.

This already leads to the first new term: a one-to-many relationship between two
models is called a **connection**.

<InfoBox type="info">

*This is markdown*
```js
console.log('hi');
```

</InfoBox>

<InfoBox type="warning">

*This is markdown*
```js
console.log('hi');
```

</InfoBox>

<InfoBox type="danger">

*This is markdown*
```js
console.log('hi');
```

</InfoBox>

## Trolo

### Example

Let's consider this following simple GraphQL query. It fetches the `releaseDate`
of the movie "Inception" and the names of all of its actors. The `actors` field is
a connection between a movie and multiple actors.

**Stuf**

This is another paragraph

Bla

* point 1
* point 2
* point 3

## Halli Hallo


HI

### Edges and nodes

#### WHOOT

Okay, let's see what's going on here. The `actors` connection now has a more
complex structure containing the fields edges and node. These terms should be a
bit more clear when looking at the following image.
