---
alias: ua9gai4kie
description: Learn how to deploy your Prisma services with Prisma Cloud.
---

# Setting up Clusters with Prisma Cloud and AWS RDS

This guide gets you up and running with [Prisma Cloud](https://www.prismagraphql.com/cloud/) and [AWS Relational Database Service](https://aws.amazon.com/rds/) (RDS).

> AWS RDS is eligible for the AWS Free Tier. This is great for development clusters or to just get things started. If you're looking for more advanced features we recommend using [AWS Aurora](https://aws.amazon.com/rds/aurora) instead.

In the following sections, you are going to learn how to:

- Provision a MySQL database instance using AWS RDS
- Set up a new Prisma cluster using the Prisma Cloud Console
- Use the MySQL database as a backing data store for your Prisma cluster

## 1. Signing up for AWS

<Instruction>

If you don't have an AWS account yet, [sign up here](https://aws.amazon.com/).

</Instruction>

This process might take a little while, but you'll get there! Note that you'll have to provide credit card info as well as your phone number at some point during the process.

## 2. Creating a new AWS RDS instance

In this section, you'll spin up a new AWS RDS instance in the `us-east-1` AWS region. If you prefer, feel free to choose any of the other supported regions instead:

* `us-east-1` / US East (N. Virgina)
* `us-west-2` / US West (Oregon)
* `ap-northeast-1` / Asia Pacific (Tokyo)
* `eu-west-1` / EU West (Ireland)

### 2.1 Get started

<Instruction>

To setup a new RDS instance in the `us-east-1` region, go [here](https://us-east-1.console.aws.amazon.com/rds/home?region=us-east-1#gettingStarted:) and click **Get Started Now**.

</Instruction>

### 2.2 Select DB engine

<Instruction>

In the **Select engine** dialog, select **MySQL**. Then click **Next**.

</Instruction>

![](https://imgur.com/RVQS5CW.png)

> **Note**: Some of the displayed engine options are not eligible for the RDS Free Usage Tier. If you want to select a different DB and ensure it's free, you can check the **Only enable options eligible for RDS Free Usage Tier** checkbox.

### 2.3 Choose use case

<Instruction>

When prompted to select a use case, select the **Dev/Test - MySQL** option for the purpose of this tutorial.

</Instruction>

### 2.4 Specify DB details

<Instruction>

In the **Specify DB details** dialog, choose the most recent **MySQL 5.7.\*** version from the dropdown for the **DB engine version**.

</Instruction>

<Instruction>

Select anything you like for the **DB instance class** - a `db.t2.micro - 1 vCPU, 1GiB RAM` instance will definitely do for this tutorial

</Instruction>

<Instruction>

Leave the following settings as they are and enter a **DB instance identifier**, a **Master username** and a **Master password**, for example:

- **DB instance identifier**: `my-prisma-demo`
- **Master username**: `myusername`
- **Master password**: `prismagraphql`

</Instruction>

<Instruction>

Then click **Next**.

</Instruction>

![](https://imgur.com/7pYA5uo.png)

### 2.5 Configure advanced settings

<Instruction>

In the **Configure advanced settings** dialog, activate public accessiblity by selecting the **Yes** checkbox.

</Instruction>

![](https://imgur.com/gSsLqaz.png)

<Instruction>

Leave the following settings as they are and scroll down to the **Backup** section. Set the **Backup retention period** to **0 days** in dropdown. This means there will be no automatic backups for this database instance.

</Instruction>

![](https://imgur.com/viVXbp8.png)

No other changes need to be made for the remaining sections, but feel free to adjust them as you wish. Then launch your DB instance by clicking the **Launch DB instance** button.

![](https://imgur.com/OiBdWtp.png)

> **Note**: In this guide, we disable backup options but you can setup any backup retention period you want. Consider using AWS Aurora instead if you're interested in more options here.

### 2.6 Instance settings

Once the instance is available, go ahead and click **View DB instance details**.

![](https://imgur.com/hERfJiF.png)

<Instruction>

In your instance settings, locate the **Endpoint** and take note of it for later. You will need it when setting up the cluster.

</Instruction>

If it is not shown here, that means your DB is still being setup. In this case, don't worry and carry on. You can collect the endpoint afterwards.

<Instruction>

Then, find the **Details** section and open the security groups by clicking the link right beneath the **Security groups** label.

</Instruction>

![](https://imgur.com/bAc1L3q.png)

<Instruction>

In the security group settings, select the **Inbound** tab at the bottom of the page and verify that a `Custom TCP Rule` inbound rule is listed. **It's important that it is set to source `0.0.0.0/0`.**

</Instruction>

![](https://imgur.com/pJ1iXYM.png)

<Instruction>

If such an inbound rule does not exist, click the **Edit** button and setup a new inbound rule with the source `Anywhere`. Click **Save**.

</Instruction>

![](https://imgur.com/wmu4Ucw.png)

## 3. Creating a Prisma cluster

> **Note**: Creating your own Prisma Cluster requires you to provide valid credit card information. Pricing starts at  **$45 / month** per cluster in a **pay-as-you-go** fashion. Find more info about the pricing [here](https://www.prismagraphql.com/cloud/pricing).

### 3.1 Signing into Prisma Cloud

<Instruction>

Navigate to the [Prisma Cloud Console](https://app.prisma.sh) and login or sign up.

</Instruction>

### 3.2 Cluster View

<Instruction>

To create a new Prisma cluster that is connected to your newly created MySQL database, navigate to the **Clusters** view by selecting the corresponding tab in the top-menu.

</Instruction>

![](https://imgur.com/qKOvKKs.png)

<Instruction>

To move on to the form where you can provide the details about your database to connect it with your cluster, click the **Create Clusters** button.

</Instruction>

![](https://imgur.com/7qDNQP2.png)

### 3.3 Database and cluster information

> **Note**: To learn more about the required information to create a new cluster, you can check out this 3-min tutorial [video](https://www.youtube.com/watch?v=jELE4KXJPn4&lc).

<Instruction>

Insert the required data:

- **Host**: This is the _endpoint_ from the previous section **2.6 Instance settings**
- **Port**: If you haven't made any changes, the default port if **3306**
- **User**: The username you chose in section **2.4 Specify DB details** (e.g. `myusername`)
- **Password**: The password you chose in section **2.4 Specify DB details** (e.g. `prismagraphql`)

</Instruction>

![](https://imgur.com/kABLZZb.png)

<Instruction>

When you're done with that, select click **Test connection**.

</Instruction>

<Instruction>

After the connection was successfully tested, you need to provide a **Cluster name** (e.g. `my-cluster`) and optionally a **Description**.

</Instruction>

<Instruction>

Finally, you need to select a **Cluster region** - choose the one that's closest to where the majority of your users is supposably located.

</Instruction>

<Instruction>

If you haven't done so before, you need now need to provide your credit card information. Finally, click **Create Cluster**.

</Instruction>

### 3.4 Provisioning a cluster

The process of setting up a cluster usually shouldn't take longer than a few seconds. The provisioning status of your cluster will be printed continuously.

![](https://imgur.com/JYKXoEM.png)

You can navigate back to the cluster overview and follow the provisioning status. Once your cluster is up and running, the status says **Healthy**.

![](https://imgur.com/ZWGfWYj.png)

## 4. Deploying a Prisma service to the cluster

Now that you provisioned a cluster, you can use it as a runtime environment for your Prisma services by using it as a deployment target.

<Instruction>

Install the latest CLI version with the following command:

```sh
npm install -g prisma1
```

</Instruction>

<Instruction>

Then, log into your Prisma Cloud account:

```sh
prisma1 login
```

</Instruction>

This will store your _cloud session key_ in `~/.prisma/config.yml`. This key is used by the CLI to authenticate you against Prisma Cloud.

<Instruction>

Next, go ahead and create a new service:

```sh
prisma1 init hello-world
```

</Instruction>

<Instruction>

When prompted by the CLI, choose the `Minimal setup: database-only` option. Then navigate into the newly created directory and deploy the service:

```sh
cd hello-world
prisma1 deploy
```

</Instruction>

After running `prisma1 deploy`, the Prisma CLI will prompt you to choose a cluster you'd like to use as a deployment target. Among the options, you'll find the cluster that you've just setup.

<Instruction>

Select the cluster that you just created in the CLI.

</Instruction>

This is it - your Prisma service is now deployed to your own Prisma Cloud cluster 🚀
