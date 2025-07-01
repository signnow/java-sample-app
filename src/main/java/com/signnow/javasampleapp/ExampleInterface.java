package com.signnow.javasampleapp;

import com.signnow.core.exception.SignNowApiException;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;

public interface ExampleInterface {

    /**
     * Handle GET request for the example.
     * @param queryParams Query parameters from the request.
     * @return The HTML body of the example.
     */
    ResponseEntity<String> handleGet(Map<String, String> queryParams) throws IOException, SignNowApiException;

    /**
     * Handle POST request for the example.
     * @param formData Form data from the request.
     * @return The HTML body of the example.
     */
    ResponseEntity<?> handlePost(String formData) throws IOException, SignNowApiException;
}
