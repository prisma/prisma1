# Roadmap

This document contains a rough outline of future features and changes in Prisma. Its primary purpose is to provide visibility into the current efforts of the Prisma engineering team and make Prisma users aware of upcoming changes.

The roadmap is updated every two weeks (i.e. with every Prisma release). If you want to see the changes that have happened in the latest releases, check out the [changelog](https://github.com/prisma/prisma/releases).

## Q2/Q3 2019

### Short term fixes & improvements

The following lists represent a number of smaller-scope issues and improvements that we are currently working on. While we're trying to be as accurate as possible, our priorities might shift and the list may be adjusted in a future release cycle.

#### Improvements
- [ ] [Unique constraint violated error on non-unique field #4440](https://github.com/prisma/prisma/issues/4440)
- [ ] [prisma on json field with empty\invalid content throws internal error #4375](https://github.com/prisma/prisma/issues/4375)
- [ ] [`prisma delete` doesn't actually delete the database #4451](https://github.com/prisma/prisma/issues/4451)
- [ ] [Nested upsert in create mutations (correct: nested connectOrCreate) #2194](https://github.com/prisma/prisma/issues/2194)
- [ ] [Allow for custom IDs to be submitted when creating new records in client API #3839](https://github.com/prisma/prisma/issues/4219)
- [ ] [[Mongo] improve relational link design to enhance performance #3754](https://github.com/prisma/prisma/issues/3754)
- [ ] [Add inner connection fields #1780](https://github.com/prisma/prisma/issues/1780)

### Specification phase

The "specification phase" reflects the spec work we do before starting to actually implement a feature. All important features are being specced out and discussed on GitHub. Please join the discussions and share your opinions with us.

Here are the big features that are currently being specced out:

- [A more fine-grained and powerful migration system](https://github.com/prisma/rfcs/blob/migrations/text/0000-migrations.md)
- [An aggregations API](https://github.com/prisma/rfcs/blob/prisma-basic-aggregation-support/text/0000-prisma-basic-aggregation-support.md)
- [A new generator API for the Prisma client](https://github.com/prisma/rfcs/blob/client-generators/text/0000-client-generators.md)
- [Datamodel v2](https://github.com/prisma/rfcs/blob/datamodel/text/0000-datamodel.md)

### Implementation phase

The "implementation phase" reflects the larger features that we are currently working on. These features typically require multiple weeks/months to be implemented:

- [Enabling usage of the Prisma client without running an extra server](https://github.com/prisma/prisma/issues/2992) (i.e. rewriting the Prisma core in Rust)
- [A more powerful API for the Prisma client (JS/TS)](https://github.com/prisma/rfcs/blob/new-ts-client-rfc/text/0000-new-ts-client.md)

## Upcoming

While the following features are currently not listed in the `Q2/Q3 2019`-section, they may still get added to it:

- Prisma SDK
- Observability & monitoring
- Caching
- Subscriptions & clustering
- Query analytics
