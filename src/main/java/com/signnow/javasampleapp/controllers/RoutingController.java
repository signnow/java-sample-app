package com.signnow.javasampleapp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.javasampleapp.ExampleInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class RoutingController {

    @GetMapping("/")
    public ResponseEntity<String> root() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("/static/error.html")) {
            if (inputStream == null) {
                return ResponseEntity.status(404)
                        .header("Content-Type", "text/html")
                        .body("<html><body><h1>404 - Page Not Found</h1></body></html>");
            }
            String html = new String(inputStream.readAllBytes());
            return ResponseEntity.status(404)
                    .header("Content-Type", "text/html")
                    .body(html);
        }
    }

    @GetMapping("/samples/{exampleName}")
    public ResponseEntity<String> routeExample(
            @PathVariable String exampleName,
            @RequestParam Map<String, String> queryParams
    ) throws IOException {
        if (!exampleName.matches("^[a-zA-Z0-9_]+$")) {
            try (var inputStream = getClass().getResourceAsStream("/static/error.html")) {
                if (inputStream == null) {
                    return ResponseEntity.status(404)
                            .header("Content-Type", "text/html")
                            .body("<html><body><h1>404 - Page Not Found</h1></body></html>");
                }
                String html = new String(inputStream.readAllBytes());
                return ResponseEntity.status(404)
                        .header("Content-Type", "text/html")
                        .body(html);
            }
        }
        String controllerPath = "com.signnow.samples." + exampleName + ".IndexController";
        try {
            Class<?> controllerClass = Class.forName(controllerPath);
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            if (controllerInstance instanceof ExampleInterface) {
                return ((ExampleInterface) controllerInstance).handleGet(queryParams);
            }
        } catch (Exception e) {
            // Log the error
            System.err.println("Error loading example controller: " + e.getMessage());
        }
        try (var inputStream = getClass().getResourceAsStream("/static/error.html")) {
            if (inputStream == null) {
                return ResponseEntity.status(404)
                        .header("Content-Type", "text/html")
                        .body("<html><body><h1>404 - Page Not Found</h1></body></html>");
            }
            String html = new String(inputStream.readAllBytes());
            return ResponseEntity.status(404)
                    .header("Content-Type", "text/html")
                    .body(html);
        }
    }

    @PostMapping("/api/samples/{exampleName}")
    public ResponseEntity<?> handleFormSubmission(
            @PathVariable String exampleName,
            @RequestBody String formData) throws IOException {
        if (!exampleName.matches("^[a-zA-Z0-9_]+$")) {
            try (var inputStream = getClass().getResourceAsStream("/static/error.html")) {
                if (inputStream == null) {
                    return ResponseEntity.status(404)
                            .header("Content-Type", "text/html")
                            .body("<html><body><h1>404 - Page Not Found</h1></body></html>");
                }
                String html = new String(inputStream.readAllBytes());
                return ResponseEntity.status(404)
                        .header("Content-Type", "text/html")
                        .body(html);
            }
        }
        String controllerPath = "com.signnow.samples." + exampleName + ".IndexController";
        try {
            Class<?> controllerClass = Class.forName(controllerPath);
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            if (controllerInstance instanceof ExampleInterface) {
                return ((ExampleInterface) controllerInstance).handlePost(formData);
            }
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json")
                    .body(new ObjectMapper().writeValueAsString(errorResponse));
        }
        try (var inputStream = getClass().getResourceAsStream("/static/error.html")) {
            if (inputStream == null) {
                return ResponseEntity.status(404)
                        .header("Content-Type", "text/html")
                        .body("<html><body><h1>404 - Page Not Found</h1></body></html>");
            }
            String html = new String(inputStream.readAllBytes());
            return ResponseEntity.status(404)
                    .header("Content-Type", "text/html")
                    .body(html);
        }
    }
}
