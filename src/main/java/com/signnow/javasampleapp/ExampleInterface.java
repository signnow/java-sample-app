package com.signnow.javasampleapp;

import com.signnow.core.exception.SignNowApiException;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface ExampleInterface {
    /**
     * Serve the example page.
     * @return The name of the view template to be rendered.
     */
    ResponseEntity<String> serveExample() throws IOException;

    /**
     * Handle form submission for the example.
     * @param formData Data submitted through the form.
     * @return The JSON body of the api request.
     */
    ResponseEntity<String> handleSubmission(String formData) throws IOException, SignNowApiException;
}
