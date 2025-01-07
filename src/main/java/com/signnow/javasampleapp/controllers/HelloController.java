package com.signnow.javasampleapp.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.signnow.api.auth.request.TokenGetRequest;
import com.signnow.api.auth.response.TokenGetResponse;
import com.signnow.core.ApiClient;
import com.signnow.core.exception.SignNowApiException;
import com.signnow.core.factory.SdkFactory;

@RestController
@RequestMapping("/hello")
public class HelloController {
    public String generateToken() {
        try {
            String bearerToken = "";

            ApiClient client = SdkFactory.createApiClientWithBearerToken(bearerToken);
            TokenGetResponse response = (TokenGetResponse) client.send(new TokenGetRequest()).getResponse();
            return response.getAccessToken();
        } catch (SignNowApiException e) {
            System.err.println("Exception when signNow API call TokenGetRequest");
            System.out.println("ERROR: " + e.getMessage());
            return null;
        }
    }

    @GetMapping
    public String sayHello() {
        return "Hello, worldv2!\n\n" + generateToken();
    }
}
