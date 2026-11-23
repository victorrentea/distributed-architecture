# Distributed Architecture

## graphql/
GraphQL gateway (8080) over 2 services: customers (8081, itself GraphQL over H2/JPA) and orders (8082, REST). GraphiQL at /graphiql.
- the gateway calls a service only if the query asks for its fields, at most once, both in parallel
- everything from customers sits under `Customer.details`; its selection is forwarded as is, in one call
- customers runs ONE SQL query, joining addresses only if they were asked for
`cd graphql && mvn package && java -jar <module>/target/<module>-1.jar` · try it with the Bruno collection in `graphql/bruno/`
