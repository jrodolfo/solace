package org.orgname.solace.broker.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orgname.solace.broker.api.service.DatabaseImpl;
import org.orgname.solace.broker.api.service.DirectPublisherService;
import org.orgname.solace.broker.api.service.DirectPublisherServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = Controller.class)  // Load only the Controller layer
@Import({DirectPublisherService.class})  // Import required service
@AutoConfigureMockMvc  // Ensures MockMvc is autoconfigured
class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DirectPublisherServiceImpl directPublisherServiceImpl;

    @MockBean
    private DatabaseImpl db;

    private static final String VALID_MESSAGE = "{\n" +
            "           \"userName\": \"solace-cloud-client\",\n" +
            "           \"password\": \"super-difficult\",\n" +
            "           \"host\": \"wss://mr-connection-blahblahblah.messaging.solace.cloud:443\",\n" +
            "           \"vpnName\": \"my-solace-broker-on-aws\",\n" +
            "           \"topicName\": \"solace/java/direct/system-01\",\n" +
            "\n" +
            "           \"message\": {\n" +
            "             \"innerMessageId\": \"001\",\n" +
            "             \"destination\": \"solace/java/direct/system-01\",\n" +
            "             \"deliveryMode\": \"PERSISTENT\",\n" +
            "             \"priority\": 3,\n" +
            "\n" +
            "             \"properties\": {\n" +
            "               \"property01\": \"value01\",\n" +
            "               \"property02\": \"value02\"\n" +
            "             },\n" +
            "\n" +
            "             \"payload\": {\n" +
            "               \"type\": \"binary\",\n" +
            "               \"content\": \"01001000 01100101 01101100 01101100\"\n" +
            "             }\n" +
            "\n" +
            "           }\n" +
            "         }";
    private static final String INVALID_MESSAGE = "";
    private static final String VALID_PAYLOAD_CONTENT = "01001000 01100101 01101100";
    private static final String MESSAGE_SENT = "{\"destination\":\"solace/java/direct/system-01\",\"content\":\"01001000 01100101 01101100\"}";


    @BeforeEach
    void setUp() {
        Mockito.reset(directPublisherServiceImpl);
        Mockito.reset(db);
    }

    // TODO: Fix the unit tests for the Controller

//    @Test
//    void shouldSendMessageSuccessfully() throws Exception {
//        // Mock the response from the service
//        when(directPublisherServiceImpl.sendMessage(anyString(), eq(VALID_PAYLOAD_CONTENT), any())).thenReturn(MESSAGE_SENT);
//
//        // Act & Assert: Send a POST request to the /message endpoint
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/messages/message")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(VALID_MESSAGE))
//                .andExpect(status().is2xxSuccessful())
//                .andExpect(content().string(MESSAGE_SENT)); // Validate response content
//    }
//
//    @Test
//    void shouldHandleInvalidMessage() throws Exception {
//        // Act & Assert: Send a POST request with invalid content
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/messages/message")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(INVALID_MESSAGE)) // Empty message
//                .andExpect(status().isBadRequest()); // Expecting a 400 Bad Request
//    }
//
//    @Test
//    void shouldHandleClientExceptions() throws Exception {
//        // Mock the service to throw a client exception
//        when(directPublisherService.sendMessage(anyString(), anyString(), any())).thenThrow(new RuntimeException("Client error"));
//
//        // Act & Assert: Send a POST request to trigger the mocked exception
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/messages/message")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(VALID_MESSAGE))
//                .andExpect(status().is4xxClientError()) // Expecting a 500 Internal Server Error
//                .andExpect(content().string("Client error"));
//    }
}