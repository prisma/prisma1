# Push Notifications 📲

## What it includes

- GraphQL type definitions
- Pusher integration for push notifications
- A `subscription` function for created posts

## Configure Pusher

You need to configure the following **environment variables**.

* `PUSHER_APP_ID`: Pusher app id
* `PUSHER_KEY`: Pusher key
* `PUSHER_SECRET`: Pusher secret
* `PUSHER_CLUSTER`: Pusher cluster

You can receive them after [signing up at Pusher](https://pusher.com/).

In this example, we use the channel `posts` and the event `created`, but you can adjust that freely.

## Setup

Download the example or [clone the repo](https://github.com/graphcool/graphcool):

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/push-notifications
cd push-notifications
```

Install the CLI (if you haven't already):

```sh
npm install -g graphcool@next
```

Create the Graphcool backend

```sh
graphcool init
```

## Frontend Setup

> For demonstration purposes, we're using Web Notifications. You can use Pusher for Mobile Notifications as well.

Set `APP_KEY` and `CLUSTER` in `notifications.html`. Then serve the file, for example with

```sh
python -m SimpleHTTPServer
```

and open `localhost:8000/notifications.html`.

## Data Setup

You can open the playground with `graphcool playground` and execute the following mutation to create a new post and receive a notification in the browser.

```graphql
mutation init {
  createPost(
    title: "This is the best post!"
  ) {
    id
    title
  }
}
```
