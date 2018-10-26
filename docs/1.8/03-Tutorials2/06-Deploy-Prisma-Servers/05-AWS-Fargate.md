---
alias: joofei3ahd
description: Learn how to deploy Prisma servers to AWS Fargate.
---

# AWS Fargate

[Prisma _servers_](https://www.prisma.io/docs/reference/prisma-servers-and-dbs/prisma-servers/overview-eu2ood0she) provide the runtime environment for your Prisma _services_. In this tutorial, you’re going to learn how to deploy a Prisma server to [AWS Fargate](https://aws.amazon.com/fargate/). The server will be backed by a MySQL database which you’re going deploy to AWS RDS first.

> AWS Fargate is a new tool for Amazon ECS and EKS that allows you to run [containers](http://aws.amazon.com/what-are-containers) without having to manage servers.

## Overview

When building GraphQL servers with Prisma, there are three backend components that need to be deployed:

* Your GraphQL server (commonly implemented with [graphql-yoga](https://github.com/prismagraphql/graphql-yoga))
* Your Prisma server & services
* Your database

Here is a high-level diagram indicating the architecture of your backend:

![](https://cdn-images-1.medium.com/max/2000/1*p17uapNIac8Grmm8r7wp6Q.png)

In other tutorials, you can learn how to deploy the GraphQL server using [Zeit Now](!alias-ahs1jahkee) and [Apex Up](!alias-shieghae3d). Today, you’re going to learn how to deploy a Prisma server using [AWS Fargate](https://aws.amazon.com/fargate/). Since every Prisma server is backed by a database, you’ll also learn how to deploy a database along with it.

You can deploy to Fargate in several ways. For this tutorial, you’re going to use preconfigured [CloudFormation](https://aws.amazon.com/cloudformation/) templates and the AWS Console GUI.

## 1. Deploying a MySQL database to RDS

While there are many ways to deploy databases with RDS (e.g. using the AWS GUI), you’re going to use a CloudFormattion template for this tutorial.

### 1.1. Getting the CloudFormation template

You can obtain the CloudFormation template for the MySQL database from our [database-templates](https://github.com/prismagraphql/database-templates) GitHub repository.

<Instruction>

Go ahead and clone or download the entire repo. It currently contains templates for MySQL ([aws/mysql.yml](https://github.com/prismagraphql/database-templates/blob/master/aws/mysql.yml)) and AWS Aurora ([aws/aurora.yml](https://github.com/prismagraphql/database-templates/blob/master/aws/aurora.yml)).

</Instruction>

### 1.2. Creating your CloudFormation stack

Next, you’re going to create the CloudFormation stack for the MySQL database based on the template you just downloaded.

<Instruction>

Navigate to [https://console.aws.amazon.com/cloudformation/](https://console.aws.amazon.com/cloudformation/) and login (if necessary).

</Instruction>

<InfoBox type=warning>

**Attention:** Fargate is currently only available in the **US East (N. Virginia), US East (Ohio), US West (Oregon), and EU (Ireland)** regions. So be sure to select one of these regions in the top-right corner of the AWS console!

</InfoBox>

<Instruction>

Once you have a fargate-supported region selected, click the **Create Stack **button. On the next screen, you then need to provide your CloudFormation template.

![](https://cdn-images-1.medium.com/max/2724/1*5YhGCnRB5d9PLoBnKsRXNw.png)

</Instruction>

<Instruction>

Click the **Choose File** button and select the `mysql.yml` file from the location where you previously downloaded the [database-templates](https://github.com/prismagraphql/database-templates) repo. Then click **Next**.

</Instruction>

> **Note:** If you’d like the `aurora.yml` template instead, you’ll need to [create a service linked tole in IAM]((http://docs.aws.amazon.com/cli/latest/reference/iam/create-service-linked-role.html)) using this CLI command: `aws iam create-service-linked-role aws-service-name ecs.amazonaws.com`. You can find more info about this [here](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/using-service-linked-roles.html). Thanks to [John Walsh](https://medium.com/@jwlsh) for figuring that out!

<Instruction>

On the next screen, you need to provide the **Stack name** as well as a **Database Password**. You can choose anything you like for that — note that the **Database Password** needs to be at least 8 characters long.

</Instruction>

For this tutorial, we’re choosing the following values:

* **Stack name:** `my-mysql-db`
* **Database Password**: `Prisma123` (be sure to pick a secure password and note it down, you’ll need in the next section)

![](https://cdn-images-1.medium.com/max/2770/1*Kgag1gFM31blWqGs2VNtIQ.png)

<Instruction>

You can leave the remaining parameters as they are and click **Next**.

</Instruction>

<Instruction>

On the next screen, you don’t need to configure anything and can directly click **Next** again.

</Instruction>

<Instruction>

On the last screen, go ahead and click the **Create** button to launch the stack.

</Instruction>

### 1.3. Wait ️️️️☕️

That’s it for the DB setup — you can now go and grab a coffee, it usually takes around 15 minutes until the stack is available.

![](https://cdn-images-1.medium.com/max/3456/1*b7kdadnpKB9s5ptF1pSm2g.png)

While you’re waiting, here are a few blog articles you might find interesting 😏

* [**How to wrap a REST API with GraphQL**](https://blog.graph.cool/how-to-wrap-a-rest-api-with-graphql-8bf3fb17547d)
* [**Reusing & Composing GraphQL APIs with GraphQL Bindings**](https://blog.graph.cool/reusing-composing-graphql-apis-with-graphql-bindings-80a4aa37cff5)
* [**GraphQL Server Basics: GraphQL Schemas, TypeDefs & Resolvers Explained**](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e)

### 1.4. Save database details

When setting up the Prisma server in the next section, you’ll need to connect it to the database which you just deployed. You will do so by providing a *host* and *port*.

Both pieces of information can be retrieved from the **Outputs** tab that you can navigate to once you selected the `my-mysql-db` in the stack list.

![](https://cdn-images-1.medium.com/max/2770/1*LxY7E4Gp74adGNvAUhtLsA.png)

<Instruction>

Save the **DatabasePort** and **DatabaseEndpoint** as you’ll need them in the next section.

</Instruction>

## 2. Deploying a Prisma server to Fargate

### 2.1. Getting the CloudFormation template

The Prisma server is deployed via a CloudFormation template as well. So, just like before you first need to get access to that template.

You can find it in the [prisma-templates](https://github.com/prisma/prisma-templates) GitHub repository.

<Instruction>

Clone or download that repo so you can use provide the template in the next step.

</Instruction>

### 2.2. Creating your CloudFormation stack

<Instruction>

Like in the previous section, navigate your browser to [https://console.aws.amazon.com/cloudformation](https://console.aws.amazon.com/cloudformation/) and click the **Create Stack** button (be sure to have the **US East (N. Virginia), US East (Ohio), US West (Oregon), or EU (Ireland)** region selected in the top-right corner).

</Instruction>

<Instruction>

Next, select the `fargate.yml` template you downloaded in the previous step. Then move on to the following screen by clicking **Next**.

</Instruction>

![](https://cdn-images-1.medium.com/max/2800/1*j1xcd7SDYdIZIDqZe40vdg.png)

<Instruction>

Here you need to provide some additional information about your Prisma server, e.g. which database it should connect to. Again, for the **Stack name** you can choose anything you like, for this tutorial we’re using `my-prisma-server`.

</Instruction>

**2.2.1. Set the ManagementApiSecret parameter**

The **ManagementApiSecret** is used to ensure only authorized people can deploy services to your Prisma server. The CLI needs to provide a JWT that was generated based on this secret when accessing the Prisma server (e.g. when you're running `prisma deploy`).

For the purpose of this tutorial we're choosing `protecting-my-prisma-server` as the value for **ManagementApiSecret**.

**2.2.2. Provide database connection details**

Next, you need to ensure that the Prisma server knows which database it can use to store the data belonging to its Prisma services. This will of course be the database that you deployed in the previous section.

<Instruction>

Now it’s time to reuse the data from step **1.4.:** Copy the **DatabaseEndpoint** from before and paste it into the **DbHost** field.

</Instruction>

It should look somewhat similar to this: `prisma.cvg9pd7kwxj1.us-east-1.rds.amazonaws.com`.

<Instruction>

The **DbPassword** is the **Database Password** you specified in step **1.2.**. Assuming you haven’t changed anything, that’s `Prisma123`.

</Instruction>

<Instruction>

If you previously haven’t changed anything about the port, the **DbPort** is already correctly set to `3306`.

</Instruction>

<Instruction>

Finally, the **DbUser** is just called `prisma` (unless you specified a different value for **Database Username **value in step **1.2.**).

</Instruction>

**2.2.3. Specifying the PrismaVersion parameter (must be at least 1.6.)**

Lastly, you need to provide the **PrismaVersion** which refers to the version of the Prisma Docker image you want to use.

> **Attention:** Fargate deploys only work with Prisma 1.6. and greater.

<Instruction>

Select 1.6 (or some later) version from the dropdown.

</Instruction>

![](https://imgur.com/jOpP3xm.png)

With all that information in place, go ahead and click **Next** to move on.

**2.2.4. Launch the stack**

<Instruction>

You don’t need any configurations on the next screen, so you can just skip it by clicking **Next** again.

</Instruction>

<Instruction>

On the last screen, you only need to check the checkbox at the bottom of the screen, confirming the following statement: **I acknowledge that AWS CloudFormation might create IAM resources.**

</Instruction>

![](https://i.imgur.com/Fw7uMWq.png)

<Instruction>

Finally, click the **Create** button to launch the stack.

</Instruction>

### 2.3. Wait ☕️

<iframe src="https://giphy.com/embed/88EvfARM1YaCQ" width="480" height="317" frameBorder="0" class="giphy-embed" allowFullScreen></iframe>

### 2.4. Save the server endpoint

Once the stack has been launched, you need to save the endpoint of the server.

<Instruction>

Similar to what you did with the database stack before, first select the `my-prisma-server` stack from the list and then open the **Outputs** tab for it. Then, go ahead and save the value for the **ExternalUrl** field, you will need it in the next step.

</Instruction>

![](https://cdn-images-1.medium.com/max/2780/1*q7rGd5dyfC5cQYHmdnDQVA.png)

## 3. Deploy a Prisma service to the new server

It’s time to put your new server in use and deploy a service to it.

<Instruction>

In a location of your choice, run the following command in the terminal to create the file structure for a new Prisma service:

```sh
prisma init my-prisma-service
```

</Instruction>

This command prompts first prompts you to select the Prisma server to which the new service should be deployed.

<Instruction>

From the provided option, select **Use other server** and hit **Enter**.

</Instruction>

The "other server" will be the one that you just deployed to AWS Fargate - so you need to provide the connection details to the CLI.

<Instruction>

Next, the CLI prompts you to specify the **endpoint** of the Prisma server. Here, you need to provide the **ExternalURL** from step **2.4.**. It should look similar to this: `http://my-pr-Publi-1GXX8QUZU3T89-433349553.us-east-1.elb.amazonaws.com`

</Instruction>

<Instruction>

For the following prompts, you can simply hit **Enter** to choose the suggested values.

</Instruction>

After those selections, the CLI creates a new directory called `my-prisma-service` with your project files.

Before deploying your service with `prisma deploy`, you need to ensure the Prisma CLI is authorized to access your Prisma server. To do so, you need to set the `PRISMA_MANAGEMENT_API_SECRET` environment variable in your shell. The CLI will read this environment variable and generate a JWT based on it which it uses to authenticate against the server.

<Instruction>

Set the `PRISMA_MANAGEMENT_API_SECRET` environment variable in your terminal:

```bash
export PRISMA_MANAGEMENT_API_SECRET="protecting-my-prisma-server"
```

</Instruction>

> **Note**: Depending on which shell you're using, the command for setting environment variables might look different.

<Instruction>

Next, navigate into it and run `prisma deploy`:

```sh
cd my-prisma-service
prisma deploy
```

</Instruction>

After the command has finished running, the CLI outputs the URL of your Prisma service. You can now test the Prisma services which is running on your very own Prisma server on AWS Fargate 🎉

![](https://cdn-images-1.medium.com/max/2904/1*iRGtCZwDjkogYFTA6leOLg.png)

<InfoBox>

💡 You can also manage your AWS Fargate server through the free _Prisma Cloud Connect_. Watch [this](https://www.youtube.com/watch?v=Wjt9Hy_BI2M&t=10s) 4min-video for more info.

</InfoBox>

## Summary

In this tutorial, you learned how to deploy a Prisma server with a backing database to AWS Fargate. In both cases, you were using preconfigured CloudFormation templates for the deployment process.
