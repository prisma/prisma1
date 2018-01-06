---
alias: sho6theegh
description: Raw DB Access
---

# Database Access (SQL)

TODO S: review
TODO N: polish

When deploying your Graphcool service locally with Docker, it is possible to send SQL queries directly against the connected MySQL database.

Follow these steps to open the [MySQL client](https://dev.mysql.com/doc/mysql-getting-started/en/#mysql-getting-started-connecting) in your terminal.

> In the following, we assume that you have a local Graphcool service running (as described in the [previous chapter](!alias-ohs4asd0pe)).

#### 1. Get the name of the Docker container that runs the MySQL database

In the root directory of your locally deployed Graphcool service, run the following command:

```sh
docker ps --filter name=local_graphcool-db
```

`docker ps` lists all running Docker containers. In this case, you're also adding a filter to ask for a specific Docker container whose name starts with the string `local_graphcool-db`.

The output contains several columns with properties about the Docker containers. You need to copy the output for the `NAME` column and use it in the next step.

> Note: In case no containers are printed, remove the filter and choose the Docker container that has the `mysql:*` image.

#### 2. Open MySQL client

To open the MySQL client in your terminal, run the following command. Note that you need to replace the placeholder `__CONTAINER_NAME__` with the container name from the previous step:

```sh
 docker exec -it __CONTAINER_NAME__ mysql -u root --host 127.0.0.1 --port 3306 --password=graphcool
```

#### 3. Send SQL queries to the database

Once the MySQL client is open, you can ask for the currently available databases:

```mysql
show databases;
```

This will print the following output:

```
+---------------------------+
| Database                  |
+---------------------------+
| information_schema        |
| graphcool                 |
| logs                      |
| mysql                     |
| performance_schema        |
| sys                       |
| <service-id>              |
+---------------------------+
```

From the available databases, the following three are relevant for you:

- `<service-id>`: The name of this database is the ID of your service (Note that `<service-id>` is a placeholder for a string consisting of 25 alphanumeric characters). It contains your application data (each `@model` type from your data model lives as one dedicated table in that database).
- `graphcool`: This database contains meta-information about the Graphcool service definition.
- `logs`: This database contains the logs from your integrated functions.

You can now select one of these databases with the `use` command, for example if your `<service-id>` is `cj9u6bssn00040156c8optc3e`, then run:

```mysql
use cj9u6bssn00040156c8optc3e;
```
