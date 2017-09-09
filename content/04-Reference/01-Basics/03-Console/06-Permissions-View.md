---
alias: ou3ucheewu
description: The permissions view in the Graphcool Console allows you to manage permissions for your Graphcool project to secure data and data access.
---

# Permissions View

The **Permission View** gives you access to all the defined [permissions](!alias-iegoo0heez) for your project and allows you to create and modify them.

## Permission list

All existing permissions are listed in the Permission View. They are grouped into **type and relation permissions**.

### Type permissions

Type permissions are associated to a specific [type](!alias-ij2choozae) in your project. The permission list contains various information for each type permission:

* whether the permission is applicable to **everyone or only authenticated users**
* the CRUD operation the permission is associated with - this can be either a query or a create, delete or update mutation in your [GraphQL API](!alias-heshoov3ai).
* the **fields the permission is applicable to**.

### Relations permissions

A relation permission is associated to a [relation](!alias-goh5uthoc1) in your project and can be applicable to **connecting or disconnecting operations for that relation, or both**.

> More information can be found in the [permissions](!alias-iegoo0heez) chapter.
