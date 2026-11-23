package demo;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import graphql.language.AstPrinter;
import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@Controller
public class GatewayApp {
  public static void main(String[] args) {
    new SpringApplicationBuilder(GatewayApp.class).properties("spring.graphql.graphiql.enabled=true").run(args);
  }

  final HttpSyncGraphQlClient customers;
  final RestClient orders;

  GatewayApp(@Value("${customers:http://localhost:8081}") String customersUrl,
             @Value("${orders:http://localhost:8082}") String ordersUrl) {
    customers = HttpSyncGraphQlClient.create(RestClient.create(customersUrl + "/graphql"));
    orders = RestClient.create(ordersUrl);
  }

  record Customer(long id, CompletableFuture<Map<String, Object>> fromCustomersService) {}
  record Order(int id, double total) {}

  @QueryMapping
  Customer customer(@Argument long id, DataFetchingEnvironment env) {
    Set<String> customersServiceFields = Set.of("name", "email", "addresses", "invoicingAddress");
    List<Selection> forwardedFields = env.getField().getSelectionSet().getSelections().stream()
        .filter(selection -> customersServiceFields.contains(((Field) selection).getName()))
        .toList();

    if (forwardedFields.isEmpty()) {
      return new Customer(id, completedFuture(Map.of())); // no call to customers
    }

    String fields = AstPrinter.printAstCompact(new SelectionSet(forwardedFields)); // "{name addresses{city}}"
    String graphqlRequestToCustomersService = "{customer(id: %d) %s}".formatted(id, fields);
    CompletableFuture<Map<String, Object>> call = supplyAsync(() -> callCustomers(graphqlRequestToCustomersService)); // started, not awaited
    return new Customer(id, call);
  }

  Map<String, Object> callCustomers(String query) {
    return customers.document(query).retrieveSync("customer").toEntity(Map.class);
  }

  @SchemaMapping
  CompletableFuture<Object> name(Customer c) {
    return c.fromCustomersService().thenApply(m -> m.get("name"));
  }

  @SchemaMapping
  CompletableFuture<Object> email(Customer c) {
    return c.fromCustomersService().thenApply(m -> m.get("email"));
  }

  @SchemaMapping
  CompletableFuture<Object> addresses(Customer c) {
    return c.fromCustomersService().thenApply(m -> m.get("addresses"));
  }

  @SchemaMapping
  CompletableFuture<Object> invoicingAddress(Customer c) {
    return c.fromCustomersService().thenApply(m -> m.get("invoicingAddress"));
  }

  @SchemaMapping
  CompletableFuture<List<Order>> orders(Customer c) { // runs in parallel with the customers call
    return supplyAsync(() -> callOrders(c.id()));
  }

  List<Order> callOrders(long customerId) {
    return orders.get()
        .uri("/orders?customerId={id}", customerId)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }
}
