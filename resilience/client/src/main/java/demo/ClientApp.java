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

  private final RestClient server;

  // inject Spring Boot's builder (not RestClient.create()), so the spring.http.client.* timeouts apply
  ClientApp(RestClient.Builder builder) {
    this.server = builder.baseUrl("http://localhost:8096").build();
  }

  @GetMapping("/call")
  String call() {
    return "client got: " + server.get().uri("/data").retrieve().body(String.class);
  }

  /** the server never answers, but the read timeout in application.properties cuts the wait after 10s */
  @GetMapping("/no-timeout")
  String noTimeout() {
    return "client got: " + server.get().uri("/hang").retrieve().body(String.class);
  }
}
