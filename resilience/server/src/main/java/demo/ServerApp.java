package demo;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@SpringBootApplication
@RestController
public class ServerApp {
  private static final Logger log = LoggerFactory.getLogger(ServerApp.class);

  public static void main(String[] args) {
    new SpringApplicationBuilder(ServerApp.class).properties("server.port=8096").run(args);
  }

  volatile long delayMillis = 0;
  volatile double failRate = 0;

  /** turn the knobs at runtime: POST /chaos?delayMillis=3000&failRate=0.5 */
  @PostMapping("/chaos")
  String chaos(@RequestParam(defaultValue = "0") long delayMillis, @RequestParam(defaultValue = "0") double failRate) {
    this.delayMillis = delayMillis;
    this.failRate = failRate;
    return "delay=" + delayMillis + "ms, failRate=" + failRate;
  }

  @GetMapping("/data")
  String data() throws InterruptedException {
    Thread.sleep(delayMillis);
    if (Math.random() < failRate) throw new IllegalStateException("server is having a bad day");
    return "data";
  }

  /** a server that accepted the request and then never answers (stuck on a lock, a slow query...) */
  @GetMapping("/hang")
  String hang() throws InterruptedException {
    Thread.sleep(3_600_000); // 1 hour
    return "finally!";
  }

  /** a third of the calls: 400, the caller's fault; a third: 503, a transient error; a third: OK */
  @GetMapping("/flaky")
  String flaky() {
    double dice = Math.random();
    if (dice < 1 / 3.0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "your request is wrong; sending it again won't help");
    if (dice < 2 / 3.0) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "transient error, try again");
    log.info("⬅️ 200 data");
    return "data";
  }

  /** at most 3 calls every 10 seconds; the 4th gets 429 Too Many Requests at once (see resilience4j.ratelimiter in application.properties) */
  @RateLimiter(name = "limited")
  @GetMapping("/limited")
  String limited() {
    log.info("⬅️ 200 data");
    return "data";
  }
}
