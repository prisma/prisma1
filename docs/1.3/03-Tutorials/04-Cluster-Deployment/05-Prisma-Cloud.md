---
alias: ua9gai4kie
description: Learn how to deploy your Prisma services with Prisma Cloud.
---

# Setting up Clusters with Prisma Cloud and AWS RDS

This guide gets you up and running with [Prisma Cloud](https://www.prismagraphql.com/cloud/) and [AWS Relational Database Service](https://aws.amazon.com/rds/) (RDS).

> AWS RDS is eligible for the AWS Free Tier. This is great for development clusters or to just get things started. If you're looking for more advanced features we recommend using [AWS Aurora](https://aws.amazon.com/rds/aurora) instead.

## 1. Signing up to AWS

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

### 2.1 Get Started

<Instruction>

To setup a new RDS instance in the `us-east-1` region, go [here](https://us-east-1.console.aws.amazon.com/rds/home?region=us-east-1#gettingStarted:) and click **Get Started Now**.

</Instruction>

### 2.2 Select DB Engine

<Instruction>

In the **Select engine** dialog, select **MySQL**. Then click **Next**.

</Instruction>

![](https://imgur.com/RVQS5CW.png)

> **Note**: Some of the displayed engine options are not eligible for the RDS Free Usage Tier. If you want to select a different DB and ensure it's free, you can check the **Only enable options eligible for RDS Free Usage Tier** checkbox.

### 2.3 Choose use case

<Instruction>

When prompted to select a use case, select the **Dev/Test - MySQL** option for the purpose of this tutorial.

</Instruction>

### 2.4 Specify DB Details

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

### 2.6 Instance Settings

Once the instance is available, go ahead and click **View DB instance details**.

![](https://imgur.com/hERfJiF.png)

<Instruction>

In your instance settings, locate the **Endpoint** and take note of it for later.

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

## 3. Creating a Prisma Cluster

You can follow [this guide](https://gist.github.com/marktani/2cbbe6467cb66bc9959b63313a248988) to create a Prisma Cluster that is connected to your newly created MySQL database.

## Questions and Feedback

Send us an [email](mailto:nilan@graph.cool) if you have any questions along the way, or want to share some feedback with us.