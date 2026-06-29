package com.signnow.javasampleapp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Runtime config holder for sample controllers that are instantiated via reflection
 * and cannot receive Spring-injected dependencies. Spring populates the static fields
 * at startup through {@link Holder}; controllers read them via the static accessors.
 */
public final class AppConfig {

    private AppConfig() {}

    private static volatile String baseUrl = "http://localhost:8080";
    private static volatile String demoUserEmail = "example@example.com";
    private static volatile String demoUserPassword = "example";

    public static String baseUrl() {
        return baseUrl;
    }

    public static String sampleUrl(String exampleName) {
        return baseUrl + "/samples/" + exampleName;
    }

    public static String demoUserEmail() {
        return demoUserEmail;
    }

    public static String demoUserPassword() {
        return demoUserPassword;
    }

    @Component
    static class Holder {

        Holder(@Value("${app.base-url:http://localhost:8080}") String baseUrl,
               @Value("${signnow.api.demo_user_email:example@example.com}") String demoUserEmail,
               @Value("${signnow.api.demo_user_password:example}") String demoUserPassword) {
            AppConfig.baseUrl = trimTrailingSlash(baseUrl);
            AppConfig.demoUserEmail = demoUserEmail;
            AppConfig.demoUserPassword = demoUserPassword;
        }

        @PostConstruct
        void ready() {
            // Touch volatile fields to publish writes; initialization happens in the constructor.
        }

        private static String trimTrailingSlash(String url) {
            if (url == null || url.isEmpty()) return url;
            return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }
    }
}
