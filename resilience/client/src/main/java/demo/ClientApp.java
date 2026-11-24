package demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@RestController
public class ClientApp {
  public static void main(String[] args) {
    new SpringApplicationBuilder(ClientApp.class).properties("server.port=8095").run(args);
  }

  private final RestClient server = RestClient.create("http://localhost:8096");

  @GetMapping("/call")
  String call() {
    return "client got: " + server.get().uri("/data").retrieve().body(String.class);
  }
}
