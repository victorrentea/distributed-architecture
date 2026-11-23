package demo;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import graphql.language.AstPrinter;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
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

  record Customer(long id) {}
  record Order(int id, double total) {}

  @QueryMapping
  Customer customer(@Argument long id) {
    return new Customer(id); // no call yet
  }

  @SchemaMapping
  CompletableFuture<Map<String, Object>> details(Customer customer, DataFetchingEnvironment env) {
    String fields = AstPrinter.printAstCompact(env.getField().getSelectionSet()); // "{name addresses{city}}"
    String graphqlRequestToCustomersService = "{customer(id: %d) %s}".formatted(customer.id(), fields);
    return supplyAsync(() -> callCustomers(graphqlRequestToCustomersService));
  }

  Map<String, Object> callCustomers(String query) {
    return customers.document(query).retrieveSync("customer").toEntity(Map.class);
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
