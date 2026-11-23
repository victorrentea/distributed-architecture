package demo;

import java.util.List;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class OrdersApp {
  public static void main(String[] args) {
    new SpringApplicationBuilder(OrdersApp.class).properties("server.port=8082").run(args);
  }

  record Order(int id, double total) {}

  @GetMapping("/orders")
  List<Order> ofCustomer(@RequestParam int customerId) {
    return List.of(new Order(customerId * 10 + 1, 99.5), new Order(customerId * 10 + 2, 15));
  }
}
