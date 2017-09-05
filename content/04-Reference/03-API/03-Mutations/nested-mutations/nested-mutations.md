---
alias: ubohch8quo
path: /docs/reference/simple-api/nested-mutations
layout: REFERENCE
shorttitle: Nested Mutations
description: Create or connect multiple nodes across relations all in a single mutation.
simple_relay_twin: yoo8vaifoa
tags:
  - simple-api
  - mutations
related:
  further:
    - wooghee1za
    - cahkav7nei
    - koo4eevun4
    - ofee7eseiy
    - zeich1raej
  more:
    - cahzai2eur
    - dah6aifoce
---

# Nested Mutations in the Simple API

When creating or updating nodes, you can execute _nested mutations_ to interact with connected parts of your type schema.

* to **create and connect to a new node** on the other side of a relation, you can use [nested create mutations](!alias-vaet3eengo).
* to **connect to an existing node** on the other side of a relation, you can use [nested connect mutations](!alias-tu9ohwa1ui).

## Limitations

Different limitations and improvement suggestions are available. Please join the discussion on GitHub!

* [Nested delete mutations](https://github.com/graphcool/feature-requests/issues/42) are not available yet. Neither are [cascading deletes](https://github.com/graphcool/feature-requests/issues/47).
* Currently, the [maximum nested level is 3](https://github.com/graphcool/feature-requests/issues/313). If you want to nest more often than that, you need to split up the nested mutations into two separate mutations.

Many other [suggestions and improvements](https://github.com/graphcool/feature-requests/issues/127) are currently being discussed.
