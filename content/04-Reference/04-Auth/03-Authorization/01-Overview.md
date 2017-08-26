---
alias: iegoo0heez
description: Graphcool features a simple yet powerful permission system that integrates seamlessly with the available authentication solutions.
---

# Authorization

Graphcool features a simple yet powerful permission system that integrates seamlessly with the available [authentication concept](!alias-geekae9gah).

> For a better getting started experience, every new Graphcool project is prepopulated with permissions that allow all operations. **Make sure to adjust the permission setup before your app goes to production**.

## Whitelist Permissions for Modular Authorization

In general, permissions follow a **whitelist approach**:

* *no operation is permitted unless explicitely allowed*
* *a permission cannot be nullified by other permissions*

Essentially this means that a request is only executed if and only if it *matches* at least one specified permission. This allows us to specify permissions in a modular way and to focus on a specific use case in a single permission which leads to many simple permissions instead of fewer complex ones.

## Request Matching for Permissions

When does a permission match a request? This is determined by the [permission parameters](!alias-soh5hu6xah). Additional conditions can be defined using [permission queries](!alias-iox3aqu0ee).
