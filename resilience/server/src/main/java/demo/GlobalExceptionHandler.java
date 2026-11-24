package demo;

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

  @ExceptionHandler(Exception.class)
  ResponseEntity<String> onAnyOther(Exception e) {
    log.error("⬅️ 500 {}", e.toString());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
  }
}
