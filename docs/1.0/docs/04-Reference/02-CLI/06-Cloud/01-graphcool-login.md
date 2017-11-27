---
alias: uz0phae1ph
description: Sign up or login
---

# `graphcool login`

Sign up or login (opens your browser for authentication). The platform token that's received after successful login will be stored in `~/.graphcoolrc`.

#### Usage

```sh
graphcool login [flags]
```

#### Flags

```
-t, --token TOKEN    System token
```

#### Examples

##### Authenticate using the browser.

```sh
graphcool login
```

##### Authenticate using an existing authentication token.

```sh
graphcool auth -t <token>
```
