# Distributed Architecture

## graphql/
GraphQL gateway (8080, GraphiQL at /graphiql) over 2 services: customers (8081) and orders (8082).
Each service is called only if the query asks for its fields, at most once, and both in parallel.
`cd graphql && mvn package && java -jar <module>/target/<module>-1.jar`
