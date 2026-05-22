package com.signnow.javasampleapp.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RoutingController.class, GlobalExceptionHandler.class})
class RoutingControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void root_returns404ErrorPage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void getExample_withInvalidName_returns404() throws Exception {
        mvc.perform(get("/samples/has-dash"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExample_withPathTraversal_returns404() throws Exception {
        mvc.perform(get("/samples/..%2Fevil"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExample_withUnknownButValidName_returns404() throws Exception {
        mvc.perform(get("/samples/NoSuchSample_xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postExample_withInvalidName_returns404() throws Exception {
        mvc.perform(post("/api/samples/bad-name")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}
