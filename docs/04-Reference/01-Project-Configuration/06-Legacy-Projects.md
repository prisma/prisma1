---
alias: aemieb1aev
description: Legacy projects are projects that have been created prior to CLI version 0.4 and have some special characteristics.
---

# Legacy Projects

A remote Graphcool project (i.e. a project in your Graphcool account) can be in either of two modes:

- _Not ejected_: Projects that are or have been created with any CLI version lower than 0.4 or in the Console are _non-ejected_ by default. These are called **legacy projects**.
- _Ejected_: Projects that are created with any CLI version greater or equal to 0.4 are _ejected_ by default. 


> These two modes exist due to the recent changes to the Graphcool platform and its new focus towards managing projects through the CLI rather than using the web-based Console. 

Legacy projects can only use CLI versions lower than 0.4 and are primarily managed through the Console. Particularly, managing _functions_ and _permissions_ can only be done in the Console.

Managing the GraphQL type definitions can still be done both in the Console (in the _Schema Editor_) and the old CLI (using `graphcool pull` and `graphcool push`).

## How to eject a project

To eject a project, you need to navigate to the **Project Settings** in the Console, select the **General**-tab and click the **Eject Project**-button. 

![](https://imgur.com/R9yNznK.png)

Notice that a project can _not_ be ejected if:

- it currently has an enabled _integration_
- it currently has an Request Pipeline function that uses the `PRE_WRITE` step 

<InfoBox type=warning>

Once a project was ejected, it can't be converted back to the non-ejected state any more!

</InfoBox>
