---
alias: aemieb1aev
description: Legacy projects are projects that have been created prior to CLI version 0.4 and have some special characteristics.
---

# Legacy Console Projects

There currently are two different kinds of Graphcool services / projects:

- **Legacy Console projects**: Projects that are or have been created with any CLI version lower than 0.4 or in the Console. Legacy Console projects are not using the [`graphcool.yml`](!alias-foatho8aip) service definition file, but simply pull the current data model from the old project configuration file `project.graphcool`.
- **Graphcool services**: Graphcool services that were created with any CLI version greater or equal to 0.4. 

> These two modes exist due to the recent changes to the Graphcool products and its new focus towards managing services through the CLI rather than using the web-based Console. 


## Managing legacy Console projects

Legacy Console projects can only use CLI versions lower than 0.4 and are primarily managed through the Console. Particularly, managing _functions_ and _permissions_ can only be done in the Console.

Managing the GraphQL type definitions can still be done both in the Console (in the _Schema Editor_) and the old CLI (using `graphcool pull` and `graphcool push`).


## Upgrading a legacy Console project to a Graphcool service

To upgrade a legacy Console project to a Graphcool service, you need to navigate to the **Project Settings** in the Console, select the **General**-tab and click the **Upgrade Project**-button. 

![](https://imgur.com/R9yNznK.png)

Notice that a project can _not_ be upgraded if:

- it currently has an enabled _integration_
- it currently has a Request Pipeline function that uses the `PRE_WRITE` step 

<InfoBox type=warning>

Once a project was upgraded, it can't be upgraded back to a legacy Console project any more!

</InfoBox>
