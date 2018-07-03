---
alias: tqfbfdzj7a
description: General pointers for upgrading a Prisma server
---

# Overview

Before upgrading your Prisma server, make sure to follow the general instructions here, as well as the instructions for upgrading to a specific version.

If you have any questions about upgrading your Prisma server, feel free to reach out in [the Forum](https://graph.cool/forum), [Slack](https://slack.graph.cool) or [Github](https://github.com/graphcool/prisma).

## Release Notes

Read through all the [release notes](https://github.com/graphcool/prisma/releases) for the versions between your current one and the target version you want to upgrade to. Consider which changes affect you and your project. If you are not sure about the implications of a certain change, feel free to ask for clarification as mentioned above.

## Data Backup

Before starting with the migration, run `prisma export` for all services to create a data backup. Alternatively, you might be able to use a backup feature for your database. Verify that the backup contains all data, and importantly, verify that restoring or `prisma import` works as well.

## Test Run

It is recommended to do a test run for the upgrade process in your staging environment. This is to get familiar with the upgrade process. To verify that all went as expected, run your upgraded staging environment through your entire test suite and real world queries and mutations.
