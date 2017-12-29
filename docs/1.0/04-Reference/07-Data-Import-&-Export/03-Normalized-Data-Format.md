---
alias: teroo5uxih
description: Normalized Data Format
---

# Normalized Data Format (NDF)

The Normalized Data Format (NDF) is used as an _intermediate_ data format for import and export in Graphcool services. NDF describes a specific structure for JSON.

## NDF value types

When using the NDF, data is split across three different "value types":

- **Nodes**: Contains data for the _scalar fields_ of nodes
- **Lists**: Contains data for _list fields_ of nodes
- **Relations**: Contains data to connect two nodes via a relation by their _relation fields_

## Structure

The structure for a JSON document in NDF is an object with the following two keys:

- `valueType`: Indicates the value type of the data in the document (this can be either `"nodes"`, `"lists"` or `"relations"`)
- `values`: Contains the actual data (adhering to the value type) as an array

The examples in the following are based on this data model:

```graphql
type User {
  id: String! @unique
  firstName: String!
  lastName: String!
  hobbies: [String!]!
  partner: User
}
```

### Nodes

In case the `valueType` is `"nodes"`, the structure for the objects inside the `values` array is as follows:

```js
{
  "valueType": "nodes",
  "values": [
    { "_typeName": STRING, "id": STRING, "<scalarField1>": ANY, "<scalarField2>": ANY, ..., "<scalarFieldN>": ANY },
    ...
  ]
}
```

The notations expresses that the fields `_typeName` and `id` are of type string. `_typeName` refers to the name of the SDL type from your data model. The `<scalarFieldX>`-placeholders will be the names of the scalar fields of that SDL type.

For example, the following JSON document can be used to import the scalar values for two `User` nodes:

```json
{
  "valueType": "nodes",
  "values": [
    {"_typeName": "User", "id": "johndoe", "firstName": "John", "lastName": "Doe"},
    {"_typeName": "User", "id": "sarahdoe", "firstName": "Sarah", "lastName": "Doe"}
  ]
}
```

### Lists

In case the `valueType` is `"lists"`, the structure for the objects inside the `values` array is as follows:

```js
{
  "valueType": "lists",
  "values": [
    { "_typeName": STRING, "id": STRING, "<scalarListField>": [ANY] },
    ...
  ]
}
```

The notations expresses that the fields `_typeName` and `id` are of type string. `_typeName` refers to the name of the SDL type from your data model. The `<scalarListField>`-placeholder is the name of the of the list fields of that SDL type. Note that in contrast to the scalar list fields, each object can only values only for one field.

For example, the following JSON document can be used to import the values for the `hobbies` list field of two `User` nodes:

```json
{
  "valueType": "lists",
  "values": [
    {"_typeName": "User", "id": "johndoe", "hobbies": ["Fishing", "Cooking"]},
    {"_typeName": "User", "id": "sarahdoe", "hobbies": ["Biking", "Coding"]}
  ]
}
```

### Relations

In case the `valueType` is `"relations"`, the structure for the objects inside the `values` array is as follows:

```js
{
  "valueType": "relations",
  "values": [
    [
      { "_typeName": STRING, "id": STRING, "fieldName": STRING },
      { "_typeName": STRING, "id": STRING, "fieldName": STRING }
    ],
    ...
  ]
}
```

The notations expresses that the fields `_typeName`, `id` and `fieldName` are of type string.

`_typeName` refers to a name of an SDL type from your data model. The `<relationField>`-placeholder is the name of the of the relation field of that SDL type. Since the goal of the relation data is to connect two nodes via a relation, each element inside the `values` array by itself is a pair (written as an array which always contains exactly two elements) rather than a single object as was the case for `"nodes"` and `"lists"`.

For example, the following JSON document can be used to create a relation between two `User` nodes via the `partner` relation field:

```json
{
  "valueType": "relations",
  "values": [
    [
      { "_typeName": "User", "id": "johndoe", "fieldName": "partner" },
      { "_typeName": "User", "id": "sarahdoe", "fieldName": "partner" }
    ]
  ]
}
```
