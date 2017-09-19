---
alias: soh5hu6xah
description: Permissions are described using different parameters like the operation and the applied fields.
---

# Permission Parameters

Permissions are described using different **parameters**.
- The **operation** of a permission describes what types of requests it is evaluated for. CRUD operations as derived from a *type* as well as operations for *relations* are available:
  * *type operations* are `Read nodes`, `Create nodes`, `Update nodes`, `Delete nodes`
  * *relation operations* are `Connect nodes` and `Disconnect nodes`

  > [Nested mutations](!ol0yuoz6go#nested-mutations), are broken down into multiple isolated operations. A nested mutation might need to pass a `Create Type` and multiple `Update Relation` permissions for instance.
- For most type operations, it's of interest which **fields** the permission governs while relation permissions can affect connecting, disconnecting or both operations.

  > To apply a permission to future fields as well, choose `apply to whole type` when creating a permission.
- The **audience** of a permission describes how the permission relates to the authenticated state of a request. A permission can either be open to `EVERYONE` or only to `AUTHENTICATED` users.
