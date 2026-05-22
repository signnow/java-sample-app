package com.signnow.javasampleapp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.javasampleapp.ExampleInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class RoutingController {

    private static final Logger log = LoggerFactory.getLogger(RoutingController.class);

    private static final Pattern EXAMPLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final String CONTROLLER_PACKAGE = "com.signnow.samples.";
    private static final String CONTROLLER_CLASS = ".IndexController";
    private static final String FALLBACK_ERROR_HTML =
            "<html><body><h1>404 - Page Not Found</h1></body></html>";

    @GetMapping("/")
    public ResponseEntity<String> root() throws IOException {
        return notFound();
    }

    @GetMapping("/samples/{exampleName}")
    public ResponseEntity<String> routeExample(
            @PathVariable String exampleName,
            @RequestParam Map<String, String> queryParams
    ) throws IOException, SignNowApiException {
        if (!EXAMPLE_NAME_PATTERN.matcher(exampleName).matches()) {
            log.warn("Rejected sample GET with invalid name: {}", exampleName);
            return notFound();
        }
        try {
            ExampleInterface controller = loadExampleController(exampleName);
            if (controller != null) {
                return controller.handleGet(queryParams);
            }
        } catch (ReflectiveOperationException e) {
            log.error("Failed to instantiate example controller '{}'", exampleName, e);
        } catch (Exception e) {
            log.error("Example '{}' failed to handle GET", exampleName, e);
            throw e;
        }
        return notFound();
    }

    @PostMapping("/api/samples/{exampleName}")
    public ResponseEntity<?> handleFormSubmission(
            @PathVariable String exampleName,
            @RequestBody String formData) throws IOException, SignNowApiException {
        if (!EXAMPLE_NAME_PATTERN.matcher(exampleName).matches()) {
            log.warn("Rejected sample POST with invalid name: {}", exampleName);
            return notFound();
        }
        try {
            ExampleInterface controller = loadExampleController(exampleName);
            if (controller != null) {
                return controller.handlePost(formData);
            }
        } catch (ReflectiveOperationException e) {
            log.error("Failed to instantiate example controller '{}'", exampleName, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Example not available");
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ObjectMapper().writeValueAsString(errorResponse));
        } catch (Exception e) {
            log.error("Example '{}' failed to handle POST", exampleName, e);
            throw e;
        }
        return notFound();
    }

    private ExampleInterface loadExampleController(String exampleName) throws ReflectiveOperationException {
        String controllerPath = CONTROLLER_PACKAGE + exampleName + CONTROLLER_CLASS;
        Class<?> controllerClass = Class.forName(controllerPath);
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        if (controllerInstance instanceof ExampleInterface example) {
            return example;
        }
        return null;
    }

    private ResponseEntity<String> notFound() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("/static/error.html")) {
            String html = inputStream == null
                    ? FALLBACK_ERROR_HTML
                    : new String(inputStream.readAllBytes());
            return ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        }
    }
}
