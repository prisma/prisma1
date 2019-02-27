# Roadmap

This documents contains a rough outline for future features and changes in Prisma. Its primary purpose is to provide visibility in the current efforts of the Prisma engineering team and make Prisma users aware of upcoming changes. 

The roadmap is updated every two weeks (i.e. with every Prisma release).

## Q1/Q2 2019

### Specification phase

The "specification phase" reflects the spec work we do before starting to actually implement a feature. All important features are being specified and discussed on GitHub, please join the discussions and share your opinions with us. 

Here are the big features that are currently being specced out:

- [A more fine-grained and powerful migration system](https://github.com/prisma/rfcs/blob/migrations/text/0000-migrations.md)
- [An aggregations API](https://github.com/prisma/rfcs/blob/prisma-basic-aggregation-support/text/0000-prisma-basic-aggregation-support.md)
- [A more powerful API for the Prisma client](https://github.com/prisma/rfcs/blob/prisma-basic-aggregation-support/text/0000-prisma-basic-aggregation-support.md)
- [A new generator API for the Prisma client](https://github.com/prisma/rfcs/blob/client-generators/text/0000-client-generators.md)

### Implementation phase

- [Prisma Admin](https://github.com/prisma/prisma-admin-feedback)
- [Datamodel v1.1](https://github.com/prisma/prisma/issues/3408)
- [Enabling usage of the Prisma client without running an extra server](https://github.com/prisma/prisma/issues/2992)
- A reimplementation of the Prisma core in Rust

## Upcoming

While the following features are currently not listed in the `Q1/Q2 2019`-section, they might still get added to it:

- Datamodel v2
- Prisma SDK
- Observability & monitoring
- Caching
- Subscriptions & clustering
- Query analytics