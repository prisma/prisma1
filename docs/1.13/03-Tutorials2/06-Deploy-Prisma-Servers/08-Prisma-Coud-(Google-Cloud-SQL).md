---
alias: mof4vo9gae
description: Learn how to deploy your Prisma services with Prisma Cloud.
---

# Setting up Clusters with Prisma Cloud and Google Cloud SQL

This guide gets you up and running with [Prisma Cloud](https://www.prismagraphql.com/cloud/) and [Google Cloud SQL](https://cloud.google.com/sql/).

> In Google Cloud you are able to receive 300$ credit for free for 12 months.

In the following sections, you are going to learn how to:

- Provision a MySQL database instance using Google Cloud SQL
- Set up a new Prisma cluster using the Prisma Cloud Console
- Use the MySQL database as a backing data store for your Prisma cluster

## 1 Signing up for Google Cloud

<Instruction>

If you don't have an Google account yet, [sign up here](https://cloud.google.com/).

</Instruction>

This process might take a little while, but you'll get there! Note that you'll have to provide credit card info as well as your phone number at some point during the process.

### 1.1 Creating a new Google Cloud SQL instance

In this section, you'll spin up a new Google Cloud SQL instance in the `us-east1` region. If you prefer, feel free to choose any of the other supported regions instead:

* `us-central1` / US Central (Iowa)
* `us-east1` / US East (South Carolina)
* `us-east4` / US East (Northern Virginia)
* `us-west1` / US West (Oregon)
* `southamerica-east1` / South America East (São Paulo)
* `northamerica-northeast1` / Canada North-East (Montréal)
* `europe-west1` / Belgium (St. Ghislain)
* `europe-west2` / Belgium (St. Ghislain)
* `europe-west3` / Belgium (St. Ghislain)
* `europe-west4` / Belgium (St. Ghislain)
* `australia-southeast1` / Australia (Sydney)
* `asia-east1` / Asia Pacific (Changhua County, Taiwan)
* `asia-northeast1` / Asia Pacific (Tokyo)
* `asia-south1` / India (Mumbai)

### 1.2 Get started

<Instruction>

First you need to make a project.

To setup a new Google Cloud SQL instance in the `us-east1` region, go in the navigation on the left to Storage/SQL. And click the button **Create Instance**

</Instruction>

### 1.3 Select DB type

<Instruction>

In the **Select MySQL**, click **Next**. And you will be prompted between First and Second Generation. Click **Choose Second Generation**.

</Instruction>

### 1.4 Fill in the instance details

<Instruction>

For the the purpose of this you can use whatever you want.
For example:

- **Instance ID**: prisma
- **Root password**: graphcool12345
- **Region**: `us-east1`
- **Zone**: Any

And click **Create**

</Instruction>

<Instruction>

Now your instance is being created.

</Instruction>

### 1.2 Configure access from an external IP-address

<Instruction>

Click the name of your instance, in this case `prisma`. And continue to the tab **Authorization**. Here a network needs to be added. Click **Add Network**. And put `0.0.0.0/0` in the Network input. Click **Done** to save the changes. Now Prisma Cloud should be able to reach your Google Cloud SQL instance using user `root` and the password you defined using the public ip of the instance.

The public ip can be found in the tab **Overview** of your instance located on **Connect to this instance**.

</Instruction>

# Setting up Clusters with Prisma Cloud and Google Cloud SQL

This guide gets you up and running with [Prisma Cloud](https://www.prismagraphql.com/cloud/) and [Google Cloud SQL](https://cloud.google.com/sql/).

> In Google Cloud you are able to receive 300$ credit for free for 12 months.

In the following sections, you are going to learn how to:

- Provision a MySQL database instance using Google Cloud SQL
- Set up a new Prisma cluster using the Prisma Cloud Console
- Use the MySQL database as a backing data store for your Prisma cluster

## 1 Signing up for Google Cloud

<Instruction>

If you don't have an Google account yet, [sign up here](https://cloud.google.com/). If you have you can use your Google account.

![](https://docs.google.com/drawings/d/e/2PACX-1vSqef3ZhzbSBvUbipefCw0vYeFP1DTx6PbXcjONMEIo_sQtF8rlC4oXtnKHsOr5QSsaOimIapBb2ZxS/pub?w=926&h=100)
![](https://docs.google.com/drawings/d/e/2PACX-1vRyVThd5vJfKoLx0kHUoeB0jOetiQu-v34O8FepAPvqOPFz7BWojZRDOcO0puGeFEwbJWsFS3ptEjqF/pub?w=311&h=344)

</Instruction>

This process might take a little while, but you'll get there! Note that you'll have to provide credit card info as well as your phone number at some point during the process.

![](https://docs.google.com/drawings/d/e/2PACX-1vR2kRhRrOovIoRq_KRumWPhhf23LKS3u5sZiLKofmMxChG3aIyvMyEXEL8GhZYf-IH5uqXguG6COEeO/pub?w=297&h=96)

Follow the steps to add your credit card, ...

### 1.1 Creating a new Google Cloud SQL instance

#### Create a new project

Click on the red box shown below in the image to create a new project.

![](https://docs.google.com/drawings/d/e/2PACX-1vSAfvUVtOFaWTaTYvNj2j6NipxHGzWhM627zWYJyMxtBlwAvOS-OjJPQMia3nsbp3FYwz_Higjap-u4/pub?w=805&h=100)

Create a name for your project.

![](https://docs.google.com/drawings/d/e/2PACX-1vRlUTPMhXbkD_Z3B6_DG6zIPof32eHL8tcSmq5TF5o6KAGyYfJvRZXDfaaPRc1thm-Zgfc-uCZ0908X/pub?w=462&h=272)

#### Navigate to Google Cloud SQL

![](https://docs.google.com/drawings/d/e/2PACX-1vRfcE1Rris9sIjekACbmU6k90VP3nNxDXLpMERUkFXaxP9Jw-7XOLK8oPk_bssS5j8A-Z7c7vMBbM5Y/pub?w=120&h=334)

In this section, you'll spin up a new Google Cloud SQL instance in the `us-east1` region. If you prefer, feel free to choose any of the other supported regions instead:

* `us-central1` / US Central (Iowa)
* `us-east1` / US East (South Carolina)
* `us-east4` / US East (Northern Virginia)
* `us-west1` / US West (Oregon)
* `southamerica-east1` / South America East (São Paulo)
* `northamerica-northeast1` / Canada North-East (Montréal)
* `europe-west1` / Belgium (St. Ghislain)
* `europe-west2` / Belgium (St. Ghislain)
* `europe-west3` / Belgium (St. Ghislain)
* `europe-west4` / Belgium (St. Ghislain)
* `australia-southeast1` / Australia (Sydney)
* `asia-east1` / Asia Pacific (Changhua County, Taiwan)
* `asia-northeast1` / Asia Pacific (Tokyo)
* `asia-south1` / India (Mumbai)

### 1.2 Get started

<Instruction>

To setup a new Google Cloud SQL instance in the `us-east1` region, go in the navigation on the left to Storage/SQL. And click the button **Create Instance**

![](https://docs.google.com/drawings/d/e/2PACX-1vTNgCZwLOuc2NBi3Gd2LUriOIyxMRBTbRpV3w6ByfXcfOiIDBvgsomTv3fxS84gBP9pctv9suxkl1c_/pub?w=410&h=231)

</Instruction>

### 1.3 Select DB type

<Instruction>

In the **Select MySQL**, click **Next**. And you will be prompted between First and Second Generation. Click **Choose Second Generation**.

![](https://docs.google.com/drawings/d/e/2PACX-1vS_nerpJbeE8JrHlmABvODPnbpn0nz-B4E_ADxK-dN7-nF7NHGPr6zzBRDLUOc37cBJOr1V-ixtftgv/pub?w=429&h=292)
![](https://docs.google.com/drawings/d/e/2PACX-1vQneur_q0SFy4SR20Ql8yGf5sBszE75jZpwaMzhiHjBNz4eXyRJjXjjrrzIaRrrBP9eHwAkNKJ7xGe_/pub?w=350&h=342)

</Instruction>

### 1.4 Fill in the instance details

<Instruction>

For the the purpose of this you can use whatever you want.
For example:

- **Instance ID**: prisma
- **Root password**: prisma12345
- **Region**: `us-east1`
- **Zone**: Any

![](https://docs.google.com/drawings/d/e/2PACX-1vQe9jXKBCe_KZCFn1pK9GizD3U4h-MMQ_PNa06gDajSquy0byLIx7aA5Wf1BXP5P-b40IY-Qx7cgALY/pub?w=428&h=342)


> You can change the advanced configurations. But for the purpose of this tutorial we won't get in it.

And click **Create**

</Instruction>

<Instruction>

Now your instance is being created. This can take some time.

</Instruction>

### 1.2 Configure access from an external IP-address

<Instruction>

Click the name of your instance, in this case `prisma`. 

![](https://docs.google.com/drawings/d/e/2PACX-1vRaEIf7jVHI6bF2l-NnGLNPZlLlt85xEaYruGlrzt4PS3-94BXzqCXJUM4H4sBR5ileshtc6jwx1uYt/pub?w=914&h=100)

And continue to the tab **Authorization**. 

![](https://docs.google.com/drawings/d/e/2PACX-1vTD7T6AxRezR-CJFKtP6ype_Q8W5_TSVrXXrlILnJ4Nx2E1zmjw7fF0YujF5cocPs626mLPoPtsEB7F/pub?w=462&h=179)

Here a network needs to be added. Click **Add Network** (Step 1), and a box above will appear (Step 2.). And put `0.0.0.0/0` in the Network input (Step 3.), name is optional.

![](https://docs.google.com/drawings/d/e/2PACX-1vTnBttt5s2Dl6dgp0JkZeTD7mNcxAwhTiEe6868hfRBYbBxNJRmSchpZZ02zObpjzREUWduc0Qv1s2Y/pub?w=462&h=307)

Click **Done** (Step 4.) to save the changes. Now Prisma Cloud should be able to reach your Google Cloud SQL instance using user `root` and the password you defined using the public ip of the instance.

The public ip can be found in the tab **Overview** of your instance located on **Connect to this instance**.

![](https://docs.google.com/drawings/d/e/2PACX-1vT70ZVEI7cIIhy-_LBFXWZ0quCZx-ijWIyuB5IVrCMkamwnau3OvAJX3FubapgT0Jc93_B_NLnOxLWn/pub?w=461&h=270)

</Instruction>

## 2. Creating a Prisma cluster

> **Note**: Creating your own Prisma Cluster requires you to provide valid credit card information. Pricing starts at  **$45 / month** per cluster in a **pay-as-you-go** fashion. Find more info about the pricing [here](https://www.prismagraphql.com/cloud/pricing).

### 2.1 Signing into Prisma Cloud

<Instruction>

Navigate to the [Prisma Cloud Console](https://app.prisma.sh) and login or sign up.

</Instruction>

### 2.2 Cluster View

<Instruction>

To create a new Prisma cluster that is connected to your newly created MySQL database, navigate to the **Clusters** view by selecting the corresponding tab in the top-menu.

</Instruction>

![](https://imgur.com/qKOvKKs.png)

<Instruction>

To move on to the form where you can provide the details about your database to connect it with your cluster, click the **Create Clusters** button.

</Instruction>

![](https://imgur.com/7qDNQP2.png)

### 2.3 Database and cluster information

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

### 2.4 Provisioning a cluster

The process of setting up a cluster usually shouldn't take longer than a few seconds. The provisioning status of your cluster will be printed continuously.

![](https://imgur.com/JYKXoEM.png)

You can navigate back to the cluster overview and follow the provisioning status. Once your cluster is up and running, the status says **Healthy**.

![](https://imgur.com/ZWGfWYj.png)

## 3. Deploying a Prisma service to the cluster

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

## Author

Google Cloud SQL - [Maarten Coppens](https://github.com/maarteNNNN)
