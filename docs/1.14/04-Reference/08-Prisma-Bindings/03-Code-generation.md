---
alias: cosha9rah3
description: Code generation (Codegen)
---

# Code generation (Codegen)

`prisma-binding` comes with a built-in [generator CLI](https://oss.prisma.io/content/GraphQL-Binding/03-Generator-CLIs.html) you can use to generate your binding.

## Install

Install with `npm`:

```sh
npm install -g prisma1-binding
```

Install with `yarn`:

```sh
yarn global add prisma-binding
```

## Usage

```
Usage: prisma-binding -i [input] -l [language] -b [outputBinding]

Options:
  --help                Show help                                      [boolean]
  --version             Show version number                            [boolean]
  --input, -i           Path to prisma.graphql file                    [string] [required]
  --language, -l        Language of the generator. Available languages:
                        typescript, javascript                         [string] [required]
  --outputBinding, -b   Output binding. Example: binding.ts            [string] [required]
```

## Usage with GraphQL Config

The `prisma-binding` CLI integrates with GraphQL Config. This means instead of passing arguments to the command, you can write a `.graphqlconfig.yml` file which will be read by the CLI.

For example, consider the following `.graphqlconfig.yml`:

```yaml
projects:
  myapp:
    schemaPath: src/generated/prisma.graphql
    extensions:
      prisma: prisma/prisma.yml
      codegen:
        - generator: prisma-binding
          language: typescript
          output:
            binding: src/generated/prisma.ts
```

Invoking simply `graphql codegen` in a directory where the above `.graphqlconfig` is available is equivalent to invoking the following terminal command:

```sh
prisma-binding \
  --language typescript \
  --outputBinding src/generated/prisma.ts
```

## Upgrading from `prisma-binding` v1.X

 Versions lower than 2.0 of `prisma-binding` were based on the `graphql prepare` instead of the `graphql codegen` command. Here is how you need to update your Prisma project files to account for the changes:

 **prisma.yml**

```yml
# ... other properties

hooks:
  post-deploy:
    - graphql get-schema
    - graphql codegen
```

**.graphqlconfig.yml**

```yml
projects:
  myapp:
    schemaPath: src/generated/prisma.graphql
    extensions:
      prisma: prisma/prisma.yml
      codegen:
        - generator: prisma-binding
          language: typescript
          output:
            binding: src/generated/prisma.ts
```
