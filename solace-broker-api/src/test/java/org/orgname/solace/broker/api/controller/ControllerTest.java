package org.orgname.solace.broker.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orgname.solace.broker.api.service.DirectPublisherServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Controller.class)
class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DirectPublisherServiceImpl directPublisherService;

    private static final String VALID_MESSAGE = "{\n" +
            "\t\"messageId\": \"001\",\n" +
            "\t\"destination\": \"solace/java/direct/system-01\",\n" +
            "\t\"deliveryMode\": \"PERSISTENT\",\n" +
            "\t\"priority\": 3,\n" +
            "\t\"properties\": {\n" +
            "\t\t\"property01\": \"value01\",\n" +
            "\t\t\"property02\": \"value02\"\n" +
            "\t\t},\n" +
            "\t\"payload\": {\n" +
            "\t\t\t\"type\": \"binary\",\n" +
            "\t\t\t\"content\": \"01001000 01100101 01101100 01101100 01101111 00101100 00100000 01010111 01101111 01110010 01101100 01100100 00100001\"\n" +
            "\t\t}\n" +
            "}\n";
    private static final String INVALID_MESSAGE = "";
    private static final String VALID_PAYLOAD_CONTENT = "01001000 01100101 01101100 01101100 01101111 00101100 00100000 01010111 01101111 01110010 01101100 01100100 00100001";
    private static final String MESSAGE_SENT = "{\"destination\":\"solace/java/direct/system-01\",\"content\":\"01001000 01100101 01101100 01101100 01101111 00101100 00100000 01010111 01101111 01110010 01101100 01100100 00100001\"}";

    // Final field is initialized via this constructor
    @Autowired
    public ControllerTest(DirectPublisherServiceImpl directPublisherServiceImpl) {
        this.directPublisherService = directPublisherServiceImpl;
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(directPublisherService);
    }

    @Test
    void shouldSendMessageSuccessfully() throws Exception {
        // Mock the response from the service
        when(directPublisherService.sendMessage(anyString(), eq(VALID_PAYLOAD_CONTENT), any())).thenReturn(MESSAGE_SENT);

        // Act & Assert: Send a POST request to the /message endpoint
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MESSAGE))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(MESSAGE_SENT)); // Validate response content
    }

    @Test
    void shouldHandleInvalidMessage() throws Exception {
        // Act & Assert: Send a POST request with invalid content
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_MESSAGE)) // Empty message
                .andExpect(status().isBadRequest()); // Expecting a 400 Bad Request
    }

    @Test
    void shouldHandleClientExceptions() throws Exception {
        // Mock the service to throw a client exception
        when(directPublisherService.sendMessage(anyString(), anyString(), any())).thenThrow(new RuntimeException("Client error"));

        // Act & Assert: Send a POST request to trigger the mocked exception
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/messages/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MESSAGE))
                .andExpect(status().is4xxClientError()) // Expecting a 500 Internal Server Error
                .andExpect(content().string("Client error"));
    }
}