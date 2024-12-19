package com.gradle.sdlcdemo.loancalculator.http;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PaymentControllerTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testWithInterest() throws Exception {
        mockMvc.perform(get("/resetCount"))
                .andExpect(status().is(HttpStatus.OK.value()));

        mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.payment", is(449.04)))
                .andExpect(jsonPath("$.count", is(1)));
    }

    @Test
    public void testZeroInterest() throws Exception {
        mockMvc.perform(get("/resetCount"))
                .andExpect(status().is(HttpStatus.OK.value()));

        mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.payment", is(277.78)))
                .andExpect(jsonPath("$.count", is(1)));
    }

    @Test
    public void testThatHitCounterIncrements() throws Exception {
        mockMvc.perform(get("/resetCount"))
                .andExpect(status().is(HttpStatus.OK.value()));

        mockMvc.perform(get("/payment?amount=100000&rate=3.5&years=30"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.payment", is(449.04)))
                .andExpect(jsonPath("$.count", is(1)));

        mockMvc.perform(get("/payment?amount=100000&rate=0&years=30"))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.payment", is(277.78)))
                .andExpect(jsonPath("$.count", is(2)));
    }
}
