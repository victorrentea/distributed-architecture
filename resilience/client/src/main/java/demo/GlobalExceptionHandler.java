package demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** instead of an opaque 500, tell the caller what went wrong when we called the server */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** the server answered, but with an error (4xx/5xx) */
  @ExceptionHandler(RestClientResponseException.class)
  ResponseEntity<String> onServerError(RestClientResponseException e) {
    String message = "server answered " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString();
    log.warn("⬅️ 502 " + message);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(message);
  }

  /** the server did not answer at all: down (connection refused) or too slow (read timeout) */
  @ExceptionHandler(ResourceAccessException.class)
  ResponseEntity<String> onNoAnswer(ResourceAccessException e) {
    String message = "server did not answer: " + e.getMostSpecificCause();
    log.warn("⬅️ 504 " + message);
    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(message);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<String> onAnyOther(Exception e) {
    log.error("⬅️ 500 " + e, e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("client bug: " + e);
  }
}
