package demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class CustomersApp {
  public static void main(String[] args) {
    new SpringApplicationBuilder(CustomersApp.class).properties("server.port=8081").run(args);
  }

  record Customer(String name, String email) {}

  @GetMapping("/customers/{id}")
  Customer get(@PathVariable int id) {
    return new Customer("Customer " + id, id + "@shop.ro");
  }
}
