package com.signnow.javasampleapp.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.signnow.core.exception.SignNowApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedBody(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMostSpecificCause().getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler({JsonProcessingException.class, IllegalArgumentException.class, ClassCastException.class})
    public ResponseEntity<Map<String, Object>> handleBadInput(Exception e) {
        log.warn("Rejected request due to invalid input: {}", e.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid request payload");
    }

    @ExceptionHandler(SignNowApiException.class)
    public ResponseEntity<Map<String, Object>> handleSignNowApi(SignNowApiException e) {
        log.error("SignNow API call failed", e);
        return problem(HttpStatus.BAD_GATEWAY, "Upstream SignNow API error");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
