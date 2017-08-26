---
alias: ejamaid4ae
path: /docs/reference/console/functions-view
layout: REFERENCE
description: The functions view in the Graphcool Console allows you to manage Graphcool functions in your Graphcool project.
tags:
  - console
related:
  further:
    - boo6uteemo
  more:
---

# Functions View

The **Functions View** contains all the [functions](!alias-boo6uteemo) for your project and allows you to create, modify and test them.

## Information in the function list

All your functions are listed in the Functions View. Here you can see several information about your functions:

* the **unique function name** acts as an identifier
* the **event type of a function** determines when it is invocated
* **the recent invocations of each function** give you a quick overview of recent executions
* **the function logs** give you detailed insights into past function invocations

> More information about functions and how the different event type work can be found in the [functions](!alias-boo6uteemo) chapter.

## Working with functions

* To create a new function, choose a name and event type and select one of the available triggers for the selected event type. You will be guided through the process of either entering an [inline or remote function](!alias-hohl0iy1ji).

* To change the code for a function or delete it all together, you can open the function popup by selecting the function in the list.

* For testing and debugging purposes you can use the **Test Run** option in the function popup. This allows you to directly execute functions, circumventing the usual trigger event.
