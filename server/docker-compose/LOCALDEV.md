# Development setup with docker & debugging in tandem with the CLI

This guide is for those who wish to run and test their code while developing, providing you with a tight feedbsack loop and the possibility to use a debugger to step through code.

There are two main approaches to running your code:
  - Build images with your changes to test the "natural" flow with the CLI.
  - Have the required servers running in Intellij.

The first option is useful when you developed a new feature and want to conclude your testing with without much rewiring, just give the CLI a spin with fresh images that you build and test if everything works if the CLI takes care of the images, just as regular graphcool users would utilize the local setup.

The second option is the one with a tight feedback loop and debugging capabilities, and useful during your usual feature development process.

## Image - based flow
- Start sbt in the server folder. Switch to the project you want to build the image for with `project <api | deploy>` (or stay in root if you want to build all images).
- Execute `docker`. This will build a new image.
- The image will have the tag from the `build.sbt` `betaImageTag` val. Either you just change it to the version the CLI uses there temporarily, or you retag the images manually.
  - How to find out what images the CLI uses: `graphcool local eject` and inspect the `docker-compose.yml` `image` key, e.g. `image: graphcool/graphcool-database:1.0.0-beta2`. The part behind the colon is the tag.
  - ^ OR: `graphcool local start` and `docker ps`, you can see the used images there.
- As soon as you have new images, you can execute `graphcool local start` again, which recreates containers if there are new images available on your local machine. You should see `recreating <...>` in the output somewhere, which tells you that a new image is spinning up.

## Intellij - based flow
### CLI <> Server basics
First, it is important to understand how the CLI interacts with the servers/clusters before we can go on and wire local development servers into the picture. This also helps to understand potential issues, debug setup issues yourself and spin up your own custom setup, if you wish to do so.

The CLI fundamentally relies on Docker Compose to do the heavy lifting. It renders all necessary environment variables into a Docker Compose file that contains all container definitions and calls `docker-compose up -d` (simplified) on the rendered file to spin up your local setup. You can inspect the exact setup the CLI spins up by calling `graphcool local eject`, which puts a `docker-compose.yml` and `.envrc` into your current directory, however, please note that it will render the files for the `single-server` setup, not the multi-server setup, which we're aiming for.

Notable environment variables are the `PORT` the whole setup will run on, the `SCHEMA_MANAGER_ENDPOINT` that is required for the API server to fetch schemas (in case of the single-server, the server calls itself!), SQL endpoint infos, etc. So take some time and get an overview of which env vars are present and which values they have.

For the CLI, the important endpoint is the one to deploy services (projects) to (+ grab status infos from the server). This one is on the deploy server, e.g. `database.graph.cool/cluster`. The top level server code is located in `DeployMain.scala`. *The CLI then assumes that on the same host* with a different path there is the Api endpoint, e.g. `database.graph.cool/foo/bar`, for a service with name `foo` and stage `bar` (the CLI prints out endpoints after deployment, for example).

During `graphcool deploy`, you're prompted where to deploy to. This info comes from the `~/.graphcool` folder in your home directory. The `cache.yml` file holds the mapping of the `service name + stage` to the cluster it is deployed to, so if you ever want to reset this mapping after deployment, this is the right file to do it. The clusters themselves are defined in the `config.yml`. If you have a regular local graphcool server, you will probably see a mapping `local` to port `60000` or similar in there already. Every time you deploy to `local` with the CLI, it will take the host and append the path it needs, e.g. `/cluster` to deploy a service.

### Server basics

The database beta consists of two servers that work in tandem: Api and Deploy. Both require access to a database, the former to the client databases and the other one to the system database ("management database"), much like the old system and simple servers in the framework.

The Api server will call the `/cluster/schema/foo/bar` endpoint of the deploy server to fetch the schema of project `foo` with stage `bar`. The Deploy server currently never calls the Api server.

### Putting it together

We need three things to start servers in Intellij and let the CLI run against it:
- Environment variables.
- A database.
- `config.yml` mapping.

For the first item, we recommend a `.envrc` file in the server root folder that defines all necessary variables. You can just copy paste the following:
```
export SCHEMA_MANAGER_SECRET=MUCHSECRET
export SCHEMA_MANAGER_ENDPOINT="http://localhost:8081/cluster/schema"

export SQL_CLIENT_HOST="graphcool-db"
export SQL_CLIENT_PORT="3306"
export SQL_CLIENT_USER="root"
export SQL_CLIENT_PASSWORD="graphcool"
export SQL_CLIENT_CONNECTION_LIMIT=10

export SQL_LOGS_HOST="graphcool-db"
export SQL_LOGS_PORT="3306"
export SQL_LOGS_USER="root"
export SQL_LOGS_PASSWORD="graphcool"
export SQL_LOGS_DATABASE="logs"
export SQL_LOGS_CONNECTION_LIMIT=10

export SQL_INTERNAL_HOST="graphcool-db"
export SQL_INTERNAL_PORT="3306"
export SQL_INTERNAL_USER="root"
export SQL_INTERNAL_PASSWORD="graphcool"
export SQL_INTERNAL_DATABASE="graphcool"
export SQL_INTERNAL_CONNECTION_LIMIT=10
```

You will have to execute `direnv allow` in the server folder to load the env vars.

The second part is easy. You can find a Docker Compose file, aptly named `dev.yml`, in the `server/docker-compose` folder that spins up a *transient database* container, which means that killing/removing the container with `docker-compose down` will wipe the database. *Important*: This already requires the env vars from step 1 to be loaded, as the Compose file looks for the env vars in your current shell session.

You can start the Compose setup with `make dev` when in the server root. Shut it down with `make dev-down`.

Next set up a mapping in the `~/.graphcool/config.yml` file (just append it to the file under the `clusters` top level key) - you can choose any name you want to, as long as it's unique:
```
intellij:
    host: 'http://localhost:8081'
    clusterSecret: ''
```

Then all you have to do is **to start intellij from the folder with the loaded env vars from the `.env` file** and go to `DeployMain.scala` and `ApiMain.scala` and start the servers regularly or in debug mode. The Deploy server runs fixed on 8081 and the Api server fixed on 9000, at the moment.

Then deploy a service with the CLI. You can select the name you gave the cluster in the mapping, e.g. `intellij`, and it will connect to your servers running in intellij.

Important: The endpoints to access the Api playground for a service will be printed wrong by the CLI - you need to change it to the correct port of the Api server, 9000.

### Accessing/Inspecting the DB
Using the `.envrc` variables for the MySql container, you can configure your shiny GUI tool, or you can connect to the DB via CLI: `mysql -u root -h 127.0.0.1 --port=3306 --password=graphcool` (if you use the defaults from above).

## Troubleshooting Tips

- General: `docker ps` and take a hard look, if you have too much stuff running kill everything when in doubt and start with only those contains that help. Also look at the ports in combination with the infos in the `~/.graphcool/config.yml` file.
- Reset the database! You might have stale data that has the wrong format, so wiping the mysql container completely usually helps (if you use the one wihout persistence, then killing it and removing it should do the trick). **Note that restarting the deploy server then sets up the correct database structure again.**
- Check your env vars. A common issue is that the schema manager endpoint on the deploy server is wired incorrectly.
- Make sure you start intellij from a shell session that has all the correct env vars!
