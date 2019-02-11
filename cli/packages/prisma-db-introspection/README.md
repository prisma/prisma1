## Shorthand

```
const renderedSdl = await connector.introspect(schema).renderToDatamodelString()
```

## Creating a connector

```
const connector = Connectors.create(DatabaseType.mysql, client)
const connector = Connectors.create(DatabaseType.postgres, client)
const connector = Connectors.create(DatabaseType.mongo, client)
```

The database client has to be connected and disconnected by the caller. 

## Introspecting

Introspect the database:
```
const introspection = await connector.introspect(schema)
```

Then, create an ISDL structure from the introspection:

```
const sdl: ISDL = await introspection.getDatamodel()
```

or with an existing reference model:
```
const sdl: ISDL = await introspection.getNormalizedDatamodel(referenceModel)
```

## Rendering introspection results

With prototype features enabled (V2)

```
const renderer = Renderers.create(introspection.databaseType, prototype)
const renderedSdl = renderer.render(sdl)
```

Without prototype featurs, simply use the shorthand:
```
const renderedSdl = introspection.renderToDatamodelString()
```
or with an existing reference model:
```
const renderedSdl = introspection.renderToNormalizedDatamodelString(referenceModel)
```