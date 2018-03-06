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

<Instruction>

Once your account is ready, navigate to the **Relational Database Service** in the [AWS Console](https://console.aws.amazon.com/console/home) by typing `RDS` in the **AWS Services** search bar.

</Instruction>

![](https://imgur.com/Xmkgl8O.png)

<InfoBox>

We recommend to setup your database in one of the following regions:

* `us-east-1` / US East (N. Virgina)
* `us-west-2` / US West (Oregon)
* `ap-northeast-1` / Asia Pacific (Tokyo)
* `eu-west-1` / EU West (Ireland)

</InfoBox>


### 2.1 Get Started

To setup a new RDS instance in `us-east-1` , [go here](https://us-east-1.console.aws.amazon.com/rds/home?region=us-east-1#gettingStarted:) and click "Get Started Now".

### 2.2 Select DB Engine

In the "Select engine" dialog, activate the checkbox "Only enable options eligible for RDS Free Usage Tier" and choose MySQL.

![](https://imgur.com/RVQS5CW.png)

### 2.3 Specify Details

In the "Specify Details" dialog, choose the most recent MySQL 5.7 version and enter a DB instance identifier, a master username and a master password. Click next.

![](https://imgur.com/7pYA5uo.png)

> Note: Choose a strong password for your master access, as this account will be used to access your database. Click next.

### 2.4 Configure Advanced Settings

In the "Configure advanced settings" dialog, activate public accessiblity.

![](https://imgur.com/gSsLqaz.png)

Set the backup retention period to 0 days.

![](https://imgur.com/viVXbp8.png)

No other changes need to be made for the "Database options", "Encryption", "Monitoring", and "Log exports" areas, but feel free to adjust them as you wish. Then launch your DB instance.

![](https://imgur.com/OiBdWtp.png)

> Note: In this guide, we disable backup options but you can setup any backup retention period you want. Consider using AWS Aurora instead if you're interested in more options here.

### 2.5 Instance Settings

Now click "View DB instance details".

![](https://imgur.com/hERfJiF.png)

In your instance settings, locate the endpoint and take note of it for later. If it is not shown here, that means your DB is still being setup. In this case, don't worry and carry on. You can collect the endpoint afterwards.

Then, open the security groups by clicking the link in the security groups section.

![](https://imgur.com/bAc1L3q.png)

In the security group settings, verify that a `Custom TCP Rule` inbound rule as shown is listed. It's important that it is set to source `0.0.0.0/0`.

![](https://imgur.com/pJ1iXYM.png)

If such an inbound rule does not exist, click the edit button and setup a new inbound rule with the source `Anywhere`. Click save.

![](https://imgur.com/wmu4Ucw.png)

## 3. Creating a Prisma Cluster

You can follow [this guide](https://gist.github.com/marktani/2cbbe6467cb66bc9959b63313a248988) to create a Prisma Cluster that is connected to your newly created MySQL database.

## Questions and Feedback

Send us an [email](mailto:nilan@graph.cool) if you have any questions along the way, or want to share some feedback with us.