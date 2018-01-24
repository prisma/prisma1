# Login with GitHub

This example demonstrates how to implement a GraphQL server that uses **GitHub authentication** and is based on Prisma & [graphql-yoga](https://github.com/graphcool/graphql-yoga).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/github-auth
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd github-auth
yarn install
```

### 2. Set up the GitHub OAuth application

Since the authentication mechanism is based on GitHub, you first need to create an OAuth applicatoin in your GitHub account.

1. Click your profile in the top-right corner of GitHub.com and select **Settings** from the dropdown.
1. In the left side-menu, select **Developer settings**.
1. On the new page, click the **New OAuth App** button.
1. Enter application data:
    1. **Application name**: _choose anything you like, e.g._ `auth-example`
    1. **Homepage URL**: `http://localhost:8000`
    1. **Authorization callback URL**: `http://localhost:8000/login.html`
1. Click **Register application**.
1. Don't close the page, you will need the displayed **Client ID** and **Client Secret** in the next step.

Next, you need to copy the values for the **Client ID** and **Client Secret** into your GraphQL server project. They're _used_ in [`github.ts`](./src/github.ts), but referenced as environment variables which are defined in [`.env`](./.env).

For each environment variable in `.env` that's prefixed with `GITHUB`, set the corresponding value by replacing the placeholder.

1. Replace `__GITHUB_CLIENT_ID__` with the **Client ID**, e.g. `GITHUB_CLIENT_ID="0c3d893e724b336a1131"`
1. Replace `__GITHUB_CLIENT_SECRET__` with the **Client Secret**, e.g. `GITHUB_CLIENT_SECRET="c0f4ecf2740fb3fc2dc70f9d9e33690a9d56d2gg"`

Finally, you also need to replace the `__GITHUB_CLIENT_ID__` placeholder in [`login.html`](./login.html#L6) with the value for **Client ID**.

### 3. Deploy the Prisma database service

You can now [deploy](https://www.prismagraphql.com/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1. Remove the `cluster` property from `prisma.yml`.
1. Run `yarn prisma deploy`.
1. When prompted by the CLI, select a public cluster (e.g. `prisma-eu1` or `prisma-us1`).
1. Set the value of the `PRISMA_ENDPOINT` environment variable in [`.env`](./.env#L2) to the HTTP endpoint that was printed after the previous command.

</details>

## Testing the API

### 1. Server`login.html` locally

In the root directory of the project, start a local web server, e.g. using Python and the following command:

```sh
python -m SimpleHTTPServer
```

A server is now running on `http://localhost:8000`.

### 2. Open `login.html` in your browser

In your browser, navigate to [`http://localhost:8000/login.html`](http://localhost:8000/login.html).

![](https://imgur.com/V9ppfuW.png)

> **Note**: Make sure the browser is not using `https`.

Then click the **Authenticate with GitHub** button.

### 3. Authorize application

On the next page, select the green **Authorize <your-username>** button.

![](https://imgur.com/2wFZO2D.png)

### 4. Copy the GitHub code

You now need to copy the `code` from the URL you see in the address bar (it's also printed to the developer console in your browser):

![](https://imgur.com/boYso3p.png)

This code can be used to authenticate the Github user in your GraphQL API.

### 5. Start the GraphQL server

Next you need to start the GraphQL server:

```sh
yarn start
```

The server is now running on [http://localhost:4000](http://localhost:4000) which you can open in your browser.

### 6. Send the `authenticate` mutation

Next you can send the `authenticate` mutation to the API. Notice that you need to replace the `__GITHUB_CODE__` placeholder with the GitHub code from step 4.

```graphql
mutation {
  authenticate(githubCode: "__GITHUB_CODE__") {
    token
    user {
      name
      bio
      public_repos
      public_gists
    }
  }
}
```

> In the response for that mutation, `name`, `bio`, `public_repos` and `public_gists` are directly retrieved from the GitHub API.

The `token` can be used to authenticate subsequent requests by setting it as the `Authorization` header in the bottom-left corner. See the [`auth`](../auth) and [`permissions`](../permissions) examples to learn more.
