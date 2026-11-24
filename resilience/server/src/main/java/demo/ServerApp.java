package demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class ServerApp {
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
}
