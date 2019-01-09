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

As you may know, Kubernetes comes with a primitive called `namespace`. This allows you to group your workload logically. Before applying the actual namespace on the cluster, we have to write the definition file for it. Inside our project directory, create a file called `namespace.yml` with the following content:

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

Prisma supports a [good range](https://github.com/prisma/prisma/issues/1751) of different database systems. Although we use MySQL for this tutorial, the steps can be easily adopted for a different database system, like PostgreSQL.

### Disk provisioning

Now that we have a valid namespace in which we can rage, it is time to deploy MySQL. Kubernetes separates between stateless and stateful deployments. A database is by nature a stateful deployment and needs a disk to actually store the data. So how do we tell our cluster to create a new disk on the cluster? By using a [PersistentVolumeClaim](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#persistentvolumeclaims):

```yml(path="kubernetes-demo/database/pvc.yml")
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: database-disk
  namespace: prisma
  labels:
    stage: production
    name: database
    app: mysql
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

Here we request a disk with a storage capacity of 20 GB. You can apply this PVC by executing:

```
kubectl apply -f database/pvc.yml
```

You should see a new disk in the [Disk Overview](https://console.cloud.google.com/compute/disks) on the Google Cloud Platform after a couple of seconds.

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
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: "prisma"
          ports:
            - name: mysql-3306
              containerPort: 3306
          volumeMounts:
            - name: database-disk
              readOnly: false
              mountPath: /var/lib/mysql
      volumes:
        - name: database-disk
          persistentVolumeClaim:
            claimName: database-disk
```

When applied, this definition schedules one Pod (`replicas: 1`), with a running container based on the image `mysql:5.7`, configures the environment (sets the password of the `root` user to `prisma`) and mounts the disk `database-disk` to the path `/var/lib/mysql`.

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

### Deploying the ConfigMap

The Prisma server needs some configuration, like the database connection information and which connector Prisma should use. We will deploy this configuration as a so-called `ConfigMap` which acts like an ordinary configuration file, but whose content can be injected into an environment variable:

```yml(path="kubernetes-demo/prisma/configmap.yml")
apiVersion: v1
kind: ConfigMap
metadata:
  name: prisma-configmap
  namespace: prisma
  labels:
    stage: production
    name: prisma
    app: prisma
data:
  PRISMA_CONFIG: |
    port: 4466
    # uncomment the next line and provide the env var PRISMA_MANAGEMENT_API_SECRET=my-secret to activate cluster security
    # managementApiSecret: my-secret
    databases:
      default:
        connector: mysql
        host: database
        port: 3306
        user: root
        password: prisma
        migrations: true
```

After defining the file, you can apply it via:

```sh
kubectl apply -f prisma/configmap.yml
```

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
          image: 'prismagraphql/prisma:1.14'
          ports:
            - name: prisma-4466
              containerPort: 4466
          env:
            - name: PRISMA_CONFIG
              valueFrom:
                configMapKeyRef:
                  name: prisma-configmap
                  key: PRISMA_CONFIG
```

This configuration looks similar to the deployment configuration of the MySQL database. We tell Kubernetes that it should schedule one replica of the server and define the environment variable by using the previously deployed `ConfigMap`.

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
The upcoming last step is also necessary if you want to integrate `prisma deploy` into your CI/CD process.
</InfoBox>

## Configuration of the Prisma CLI

The Prisma server is running on the Kubernetes cluster and has an internal load balancer. This is a sane security default, because you won't expose the Prisma server to the public directly. Instead, you would develop a GraphQL API and deploy it to the Kubernetes cluster as well.

You may ask: "Okay, but how do I execute `prisma deploy` in order to populate my data model when I'm not able to communicate with the Prisma server directly?". That is indeed a very good question! `kubectl` comes with a mechanism that allows forwarding a local port to an application that lives on the Kubernetes cluster.

So every time you want to communicate with your Prisma server on the Kubernetes cluster, you have to perform the following steps:

1.  `kubectl get pods --namespace prisma` to identify the pod name
2.  `kubectl port-forward --namespace prisma <the-pod-name> 4467:4466` – This will forward from `127.0.0.1:4467` -> `kubernetes-cluster:4466`

The Prisma server is now reachable via `http://localhost:4467`. This is the actual `endpoint` you have to specify in your `prisma.yml`. So when your service should have the name `myservice` and you want to deploy to stage `production`, your endpoint URL would look like: `http://localhost:4467/myservice/production`.

An example `prisma.yml` could look like:

```yml
endpoint: http://localhost:4467/myservice/production
datamodel: datamodel.graphql
```

With this in place, you can deploy the Prisma service via the Prisma CLI (`prisma deploy`) as long as your port forwarding to the cluster is active.

Okay, you made it! Congratulations, you have successfully deployed a Prisma server to a production Kubernetes cluster environment.

## Author

[André König](https://andrekoenig.de) – Freelance Software Architect & Software Engineer
