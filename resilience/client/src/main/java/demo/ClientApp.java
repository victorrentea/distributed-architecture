package demo;

import io.github.resilience4j.retry.annotation.Retry;
import java.time.Duration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@RestController
public class ClientApp {
  public static void main(String[] args) {
    new SpringApplicationBuilder(ClientApp.class).properties("server.port=8095").run(args);
  }

  private final RestClient server; // the application default: 10s, from application.properties
  private final RestClient patientServer; // for the calls we KNOW are slow: 40s

  // inject Spring Boot's builder (not RestClient.create()), so the spring.http.client.* timeouts apply
  ClientApp(RestClient.Builder builder) {
    this.server = builder
        .baseUrl("http://localhost:8096")
        .build();

    var fortySeconds = ClientHttpRequestFactorySettings.defaults()
        .withConnectTimeout(Duration.ofSeconds(1))
        .withReadTimeout(Duration.ofSeconds(40));
    this.patientServer = builder
        .baseUrl("http://localhost:8096")
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(fortySeconds))
        .build();
  }

  @GetMapping("/call")
  String call() {
    return "client got: " + server.get()
        .uri("/data")
        .retrieve()
        .body(String.class);
  }

  /** a call known to take long gets its own, larger timeout: 40s instead of the default 10s */
  @GetMapping("/slow")
  String slow() {
    return "client got: " + patientServer.get()
        .uri("/hang")
        .retrieve()
        .body(String.class);
  }

  /** the server fails half of the time: try again, up to 3 times (see resilience4j.retry in application.properties) */
  @Retry(name = "flaky")
  @GetMapping("/retry")
  String retry() {
    System.out.println(java.time.LocalTime.now() + " calling /flaky ...");
    return "client got: " + server.get()
        .uri("/flaky")
        .retrieve()
        .body(String.class);
  }
}
