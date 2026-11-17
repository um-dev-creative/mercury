package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real MVC wiring: an error on a {@code text/event-stream} endpoint must still be answered as JSON with its own status. */
class GlobalExceptionHandlerEventStreamTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new StreamProbe()).setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void invalidTokenOnAnEventStreamAnswersJson401() throws Exception {
        mvc.perform(get("/probe/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @RestController
    static class StreamProbe {
        @GetMapping(value = "/probe/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        String stream() {
            throw new InvalidSessionTokenException("Invalid or missing session token.");
        }
    }
}
