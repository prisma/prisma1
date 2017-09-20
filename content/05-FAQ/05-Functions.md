---
alias: ietheucoh2
description: Frequently asked questions everything about the integration of serverless functions with Graphcool.
---

# Functions

### In what environment are functions executed with Graphcool? Can I use ES6 for my js code?

* functions are are executed based on auth0 extend
* ES6 can be used if 'use-latest' is written on top of the function

### How can I validate and transform Requests before data gets written to the database?

* Graphcool offers two hooks to transform *and* validate data before it gets written to the DB, you can use serverless functions
    * HTTP request: validate and transform the HTTP request
    * mutation_before: validate and transform mutation arguments 


### What is the graphcool-lib and how can I use it?

* the graphcool-lib is an interface to the Graphcool system that you can use when writing inline functions with Graphcool
* here's some functionality it offers:
    * sending queries and mutations to a Graphcool project
    * generating a tmp access token for a user
    * ... ?
* you can simply import it in your inline functions using import 'graphcool-lib'

### How can I test my serverless functions?

* inline functions can be executed in the Graphcool console
* *what about unit testing?*

