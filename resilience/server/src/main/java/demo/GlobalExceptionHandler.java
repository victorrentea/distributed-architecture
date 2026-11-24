package demo;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** logs every error the server returns, so you see on this side what the client got */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<String> onResponseStatus(ResponseStatusException e) {
    log.warn("⬅️ {} {}", e.getStatusCode().value(), e.getReason());
    return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
  }

  /** the rate limiter refused the call: the caller is sending too much, it should slow down */
  @ExceptionHandler(RequestNotPermitted.class)
  ResponseEntity<String> onRateLimited(RequestNotPermitted e) {
    log.warn("⬅️ 429 {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("too many requests, slow down: " + e.getMessage());
  }

  /** the bulkhead is full: the allowed number of calls are already running at this moment */
  @ExceptionHandler(BulkheadFullException.class)
  ResponseEntity<String> onBulkheadFull(BulkheadFullException e) {
    log.warn("⬅️ 429 {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("busy, another call is in progress: " + e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<String> onAnyOther(Exception e) {
    log.error("⬅️ 500 {}", e.toString());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
  }
}
