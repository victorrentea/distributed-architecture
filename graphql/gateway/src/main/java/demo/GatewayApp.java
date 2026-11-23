package demo;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@Controller
public class GatewayApp {
  public static void main(String[] args) {
    new SpringApplicationBuilder(GatewayApp.class).properties("spring.graphql.graphiql.enabled=true").run(args);
  }

  @Value("${customers:http://localhost:8081}") String customers;
  @Value("${orders:http://localhost:8082}") String orders;
  RestClient http = RestClient.create();

  record Profile(String name, String email) {}
  record Order(int id, double total) {}

  class Customer {
    final int id;
    CompletableFuture<Profile> profile; // fetched once, only if a field needs it

    Customer(int id) { this.id = id; }

    synchronized CompletableFuture<Profile> profile() {
      if (profile == null) profile = supplyAsync(() -> http.get().uri(customers + "/customers/{id}", id).retrieve().body(Profile.class));
      return profile;
    }
  }

  @QueryMapping
  Customer customer(@Argument int id) {
    return new Customer(id); // no call yet
  }

  @SchemaMapping
  int id(Customer c) {
    return c.id;
  }

  @SchemaMapping
  CompletableFuture<String> name(Customer c) {
    return c.profile().thenApply(Profile::name);
  }

  @SchemaMapping
  CompletableFuture<String> email(Customer c) {
    return c.profile().thenApply(Profile::email);
  }

  @SchemaMapping
  CompletableFuture<List<Order>> orders(Customer c) { // runs in parallel with profile()
    return supplyAsync(() -> http.get().uri(orders + "/orders?customerId={id}", c.id).retrieve().body(new ParameterizedTypeReference<List<Order>>() {}));
  }
}
