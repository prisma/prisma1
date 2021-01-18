---
alias: aiqu8ahgha
description: Learn how to deploy your Prisma database service to Kubernetes.
---

# Kubernetes

In this tutorial, you will learn how to deploy a Prisma server on Kubernetes.

[Kubernetes](https://kubernetes.io/) is a container orchestrator, that helps with deploying and scaling of your containerized applications.

<InfoBox>

The setup in this tutorial assumes that you have a running Kubernetes cluster in place. There are several providers out there that gives you the possibility to establish and maintain a production grade cluster. This tutorial aims to be provider agnostic, because Kubernetes is actually the abstraction layer. The only part which differs slightly is the mechanism for creating `persistent volumes`. For demonstration purposes, we use the [Kubernetes Engine](https://cloud.google.com/kubernetes-engine) on the [Google Cloud Platform](https://cloud.google.com/) in this tutorial.

</InfoBox>

<InfoBox>
All Kubernetes definition files are also bundled in this [repository](https://github.com/akoenig/prisma-kubernetes-deployment)
</InfoBox>

## Prerequisites

If you haven't done that before, you need to fulfill the following prerequisites before you can deploy a Prisma cluster on Kubernetes. You need ...

* ... a running Kubernetes cluster (e.g. on the Google Cloud Platform)
* ... a local version of [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) which is configured to communicate with your running Kubernetes cluster

You can go ahead now and create a new directory on your local machine – call it `kubernetes-demo`. This will be the reference directory for our journey.

## Creating a separate namespace

As you may know, Kubernetes comes with a primitive called `namespace`. This allows you to group your applications logically. Before applying the actual namespace on the cluster, we have to write the definition file for it. Inside our project directory, create a file called `namespace.yml` with the following content:

```yml(path="kubernetes-demo/namespace.yml")
apiVersion: v1
kind: Namespace
metadata:
  name: prisma
```

This definition will lead to a new namespace, called `prisma`. Now, with the help of `kubectl`, you can apply the namespace by executing:

```sh
kubectl apply -f namespace.yml
```

Afterwards, you can perform a `kubectl get namespaces` in order to check if the actual namespace has been created. You should see the following on a fresh Kubernetes cluster:

```
❯ kubectl get namespaces
NAME            STATUS    AGE
default         Active    1d
kube-public     Active    1d
kube-system     Active    1d
prisma          Active    2s
```

## MySQL

Now that we have a valid namespace in which we can rage, it is time to deploy MySQL. Kubernetes separates between stateless and stateful deployments. A database is by nature a stateful deployment and needs a disk to actually store the data. As described in the introduction above, every cloud provider comes with a different mechanism of creating disks. In the case of the [Google Cloud Platform](https://cloud.google.com), you can create a disk by following the following steps:

1. Open the [Google Cloud Console](https://console.cloud.google.com)
2. Go to the [Disk section](https://console.cloud.google.com/compute/disks) and select `Create`

Please fill out the form with the following information:

* **Name:** Should be `db-persistence`
* **Zone:** The zone in which the Nodes of your Kubernetes cluster are deployed, e.g. `europe-west-1c`
* **Disk type:** For a production scenario `SSD persistent disk`
* **Source type:** `None (blank disk)`
* **Size (GB):** Select a size that fits your requirements
* **Encryption:** `Automatic (recommended)`

Select `Create` for actually creating the disk.

<InfoBox>

To keep things simple, we created the disk above manually. You can automate that process by provisioning a disk via [Terraform](https://www.terraform.io/) as well, but this is out of the scope of this tutorial.

</InfoBox>

### Deploying the Pod

Now where we have our disk for the database, it is time to create the actual deployment definition of our MySQL instance. A short reminder: Kubernetes comes with the primitives of `Pods` and `ReplicationControllers`.

A `Pod` is like a "virtual machine" in which a containerized application runs. It gets an own internal IP address and (if configured) disks attached to it. The `ReplicationController` is responsible for scheduling your `Pod` on cluster nodes and ensuring that they are running and scaled as configured.

In older releases of Kubernetes it was necessary to configure those separately. In recent versions, there is a new definition resource, called `Deployment`. In such a configuration you define what kind of container image you want to use, how much replicas should be run and, in our case, which disk should be mounted.

The deployment definition of our MySQL database looks like:

```yml(path="kubernetes-demo/database/deployment.yml")
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: database
  namespace: prisma
  labels:
    stage: production
    name: database
    app: mysql
spec:
  replicas: 1
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        stage: production
        name: database
        app: mysql
    spec:
      containers:
        - name: mysql
          image: 'mysql:5.7'
          args:
            - --ignore-db-dir=lost+found
            - --max-connections=1000
            - --sql-mode=ALLOW_INVALID_DATES,ANSI_QUOTES,ERROR_FOR_DIVISION_BY_ZERO,HIGH_NOT_PRECEDENCE,IGNORE_SPACE,NO_AUTO_CREATE_USER,NO_AUTO_VALUE_ON_ZERO,NO_BACKSLASH_ESCAPES,NO_DIR_IN_CREATE,NO_ENGINE_SUBSTITUTION,NO_FIELD_OPTIONS,NO_KEY_OPTIONS,NO_TABLE_OPTIONS,NO_UNSIGNED_SUBTRACTION,NO_ZERO_DATE,NO_ZERO_IN_DATE,ONLY_FULL_GROUP_BY,PIPES_AS_CONCAT,REAL_AS_FLOAT,STRICT_ALL_TABLES,STRICT_TRANS_TABLES,ANSI,DB2,MAXDB,MSSQL,MYSQL323,MYSQL40,ORACLE,POSTGRESQL,TRADITIONAL
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: "graphcool"
          ports:
            - name: mysql-3306
              containerPort: 3306
          volumeMounts:
            - name: db-persistence
              readOnly: false
              mountPath: /var/lib/mysql
      volumes:
        - name: db-persistence
          gcePersistentDisk:
            readOnly: false
            fsType: ext4
            pdName: db-persistence
```

When applied, this definition schedules one Pod (`replicas: 1`), with a running container based on the image `mysql:5.7`, configures the environment (sets the password of the `root` user to `graphcool`) and mounts the disk `db-persistence` to the path `/var/lib/mysql`.

To actually apply that definition, execute:

```
kubectl apply -f database/deployment.yml
```

You can check if the actual Pod has been scheduled by executing:

```
kubectl get pods --namespace prisma

NAME                        READY     STATUS    RESTARTS   AGE
database-3199294884-93hw4   1/1       Running   0          1m
```

It runs!

## Deploying the Service

Before diving into this section, here's a short recap.

Our MySQL database pod is now running and available within the cluster internally. Remember, Kubernetes assigns a local IP address to the `Pod` so that another application could access the database.

Now, imagine a scenario in which your database crashes. The cluster management system will take care of that situation and schedules the `Pod` again. In this case, Kubernetes will assign a different IP address which results in crashes of your applications that are communicating with the database.

To avoid such a situation, the cluster manager provides an internal DNS resolution mechanism. You have to use a different primitive, called `Service`, to benefit from this. A service is an internal load balancer that is reachable via the `service name`. Its task is to forward the traffic to your `Pod(s)` and make it reachable across the cluster by its name.

A service definition for our MySQL database would look like:

```yml(path="kubernetes-demo/database/service.yml")
apiVersion: v1
kind: Service
metadata:
  name: database
  namespace: prisma
spec:
  ports:
  - port: 3306
    targetPort: 3306
    protocol: TCP
  selector:
    stage: production
    name: database
    app: mysql
```

The definition would create an internal load balancer with the name `database`. The service is then reachable by this name within the `prisma` namespace. A little explanation about the `spec` section:

* **ports:** Here you map the service port to the actual container port. In this case the mapping is `3306` to `3306`.
* **selector:** Kind of a query. The load balancer identifies `Pods` by selecting the ones with the specified labels.

After creating this file, you can apply it with:

```sh
kubectl apply -f database/service.yml
```

To verify that the service is up, execute:

```sh
kubectl get services --namespace prisma

NAME       TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
database   ClusterIP   10.3.241.165   <none>        3306/TCP   1m
```

## Prisma

Okay, fair enough, the database is deployed. Next up: Deploying the actual Prisma server which is responsible for serving as an endpoint for the Prisma CLI.

This application communicates with the already deployed `database` service and uses it as the storage backend. Therefore, the Prisma server is a stateless application because it doesn't need any additional disk storage.

### Deploying the Pod

Deploying the actual Prisma server to run in a Pod is pretty straightforward. First of all you have to define the deployment definition:

```yml(path="kubernetes-demo/prisma/deployment.yml")
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: prisma
  namespace: prisma
  labels:
    stage: production
    name: prisma
    app: prisma
spec:
  replicas: 1
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        stage: production
        name: prisma
        app: prisma
    spec:
      containers:
        - name: prisma
          image: 'prismagraphql/prisma:1.1'
          ports:
            - name: prisma-4466
              containerPort: 4466
          env:
            - name: PORT
              value: "4466"
            - name: SQL_CLIENT_HOST_CLIENT1
              value: "database"
            - name: SQL_CLIENT_HOST_READONLY_CLIENT1
              value: "database"
            - name: SQL_CLIENT_HOST
              value: "database"
            - name: SQL_CLIENT_PORT
              value: "3306"
            - name: SQL_CLIENT_USER
              value: "root"
            - name: SQL_CLIENT_PASSWORD
              value: "graphcool"
            - name: SQL_CLIENT_CONNECTION_LIMIT
              value: "10"
            - name: SQL_INTERNAL_HOST
              value: "database"
            - name: SQL_INTERNAL_PORT
              value: "3306"
            - name: SQL_INTERNAL_USER
              value: "root"
            - name: SQL_INTERNAL_PASSWORD
              value: "graphcool"
            - name: SQL_INTERNAL_DATABASE
              value: "graphcool"
            - name: SQL_INTERNAL_CONNECTION_LIMIT
              value: "10"
            - name: CLUSTER_ADDRESS
              value: "http://prisma:4466"
            - name: BUGSNAG_API_KEY
              value: ""
            - name: ENABLE_METRICS
              value: "0"
            - name: JAVA_OPTS
              value: "-Xmx1G"
            - name: SCHEMA_MANAGER_SECRET
              value: "graphcool"
            - name: SCHEMA_MANAGER_ENDPOINT
              value: "http://prisma:4466/cluster/schema"
            - name: CLUSTER_PUBLIC_KEY
              value: "GENERATE VIA https://api.cloud.prisma.sh/"
```

This configuration looks similar to the deployment configuration of the MySQL database. We tell Kubernetes that it should schedule one replica of the server and define the environment variables accordingly. As you can see, we use the name `database` for each `SQL_*_HOST*` variable. This works because of the fact that this `Pod` will run in the same namespace as the database service – the Kubernetes DNS server makes that possible.

Before applying that definition, we have to generate a public/private-keypair so that the CLI is able to communicate with this Prisma server. Head over to [https://api.cloud.prisma.sh/](https://api.cloud.prisma.sh/) and execute the following query:

```
{
  generateKeypair {
    public
    private
  }
}
```

Make sure to store those values in a safe place! Now, copy the `public` key and paste it into the `value` of the `CLUSTER_PUBLIC_KEY` environment variable in `prisma/deployment.yml`.

Afterwards, we are ready to apply that deployment definition:

```sh
kubectl apply -f prisma/deployment.yml
```

As in the previous sections: In order to check that the Prisma server has been scheduled on the Kubernetes cluster, execute:

```sh
kubectl get pods --namespace prisma

NAME                        READY     STATUS    RESTARTS   AGE
database-3199294884-93hw4   1/1       Running   0          5m
prisma-1733176504-zlphg     1/1       Running   0          1m
```

Yay! The Prisma server is running! Off to our next and last step:

### Deploying the Service

Okay, cool, the database `Pod` is running and has an internal load balancer in front of it, the Prisma server `Pod` is also running, but is missing the load balancer a.k.a. `Service`. Let's fix that:

```yml(path="kubernetes-demo/prisma/service.yml")
apiVersion: v1
kind: Service
metadata:
  name: prisma
  namespace: prisma
spec:
  ports:
  - port: 4466
    targetPort: 4466
    protocol: TCP
  selector:
    stage: production
    name: prisma
    app: prisma
```

Apply it via:

```sh
kubectl apply -f prisma/service.yml
```

Okay, done! The Prisma server is now reachable within the Kubernetes cluster via its name `prisma`.

That's all. Prisma is running on Kubernetes!

The last step is to configure your local `Prisma CLI` so that you can communicate with the instance on the Kubernetes Cluster.

<InfoBox>

The upcoming last step is also necessary if you want to integrate `prisma1 deploy` into your CI/CD process.

</InfoBox>

## Configuration of the Prisma CLI

The Prisma server is running on the Kubernetes cluster and has an internal load balancer. This is a sane security default, because you won't expose the Prisma server to the public directly. Instead, you would develop a GraphQL API and deploy it to the Kubernetes cluster as well.

You may ask: "Okay, but how do I execute `prisma1 deploy` in order to populate my data model when I'm not able to communicate with the Prisma server directly?`. That is indeed a very good question!`kubectl` comes with a mechanism that allows forwarding a local port to an application that lives on the Kubernetes cluster.

So every time you want to communicate with your Prisma server on the Kubernetes cluster, you have to perform the following steps:

1. `kubectl get pods --namespace prisma` to identify the pod name
2. `kubectl port-forward --namespace prisma <the-pod-name> 4467:4466` – This will forward from `127.0.0.1:4467` -> `kubernetes-cluster:4466`

The Prisma server is now reachable via `http://localhost:4467`. With this in place, we can configure the CLI:

```sh
prisma1 cluster add

? Please provide the cluster endpoint http://localhost:4467
? Please provide the cluster secret <the-private-key-from-the-api-call>
? Please provide a name for your cluster kubernetes
```

Okay, you made it! Congratulations, you have successfully deployed a Prisma server to a production Kubernetes cluster environment.

## Author

[André König](https://andrekoenig.de) – Freelance Software Architect & Software Engineer
