# Test

## Blackbox Tests

These tests load a datamodel, compute the CRUD schema, and then compare it with a ground-truth CRUD schema. 
The ground-truth CRUD schema should be kept up to date accoring to the latest prisma server version. This can be done using the tools provided in the `scripts` folder. 

### Blackbox Test Cases

*airbnb* Airbnb Schema from Prisma Team

*defaultValue* Tests default values. 

*enum* Tests if enums are generated correctly, and also if unused enums are not generated. 

*meshRelation* Tests a mesh of relations. 

*financial* Financial Schema from Prisma Team

*oneSidedConnection* A schema that has all cases of one-sided connections

*relationNames* Tests if relation names are parsed correctly. 

*relations* A schema that has several random cases for relations

*scalars* Tests scalar types in various configurations. 

*selfReferencing* Tests self-referencing relations. 

*simple* A basic schema

*twoSidedConnections* A schema that has all cases of two-sided connections

*withAndWithoutId* Type model with types with ID and without ID
