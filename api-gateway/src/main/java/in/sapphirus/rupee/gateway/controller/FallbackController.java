package in.sapphirus.rupee.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Auth service is currently starting or unavailable. Please retry in a few moments."
                ));
    }

    @RequestMapping("/profile")
    public ResponseEntity<Map<String, Object>> profileFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Profile service is currently unavailable. Please retry in a few moments."
                ));
    }

    @RequestMapping("/practice")
    public ResponseEntity<Map<String, Object>> practiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Practice service is currently unavailable. Please retry in a few moments."
                ));
    }

    @RequestMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> portfolioFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Portfolio service is currently unavailable. Please retry in a few moments."
                ));
    }

    @RequestMapping("/community")
    public ResponseEntity<Map<String, Object>> communityFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Community service is currently unavailable. Please retry in a few moments."
                ));
    }

    @RequestMapping("/market")
    public ResponseEntity<Map<String, Object>> marketFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Market service is currently unavailable. Please retry in a few moments."
                ));
    }
}
