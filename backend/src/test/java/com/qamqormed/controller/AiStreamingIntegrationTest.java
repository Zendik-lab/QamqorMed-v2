package com.qamqormed.controller;

import com.qamqormed.ai.OllamaClient;
import com.qamqormed.entity.AiConversation;
import com.qamqormed.entity.AiMessage;
import com.qamqormed.entity.Role;
import com.qamqormed.entity.User;
import com.qamqormed.repository.AiConversationRepository;
import com.qamqormed.repository.AiMessageRepository;
import com.qamqormed.repository.UserRepository;
import com.qamqormed.security.JwtService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.ai.enabled=true")
class AiStreamingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private AiConversationRepository conversations;

    @Autowired
    private AiMessageRepository messages;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private OllamaClient ollamaClient;

    @Test
    void authenticatedUserReceivesSseStream() throws Exception {
        User user = user("+77001234001");
        stubStream();

        MvcResult asyncResult = mockMvc.perform(post("/api/ai/chat/stream")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\",\"language\":\"en\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("\"type\":\"delta\"")))
                .andExpect(content().string(containsString("Local answer")))
                .andExpect(content().string(containsString("\"type\":\"done\"")));
    }

    @Test
    void unauthenticatedAiStreamIsRejected() throws Exception {
        mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\",\"language\":\"en\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamReadsExistingTextHistoryBeforeCallingOllama() throws Exception {
        User user = user("+77001234002");
        AiConversation conversation = new AiConversation();
        conversation.setOwner(user);
        conversation.setOwnerRole(Role.PATIENT);
        conversation.setTitle("Existing conversation");
        conversation = conversations.save(conversation);

        AiMessage previous = new AiMessage();
        previous.setConversation(conversation);
        previous.setMessageType(AiMessage.MessageType.USER);
        previous.setContent("previous history");
        messages.save(previous);
        stubStream();

        MvcResult asyncResult = mockMvc.perform(post("/api/ai/chat/stream")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conversationId\":" + conversation.getId() + ",\"message\":\"next message\",\"language\":\"en\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<OllamaClient.OllamaMessage>> prompt =
                ArgumentCaptor.forClass((Class) java.util.List.class);
        verify(ollamaClient).stream(prompt.capture(), any());
        assertTrue(prompt.getValue().stream().anyMatch(message -> "previous history".equals(message.content())));
    }

    @Test
    void allSupportedRolesCanUseAiStream() throws Exception {
        stubStream();
        int index = 10;
        for (Role role : Role.values()) {
            User user = user("+770012340" + index++);
            user.setRole(role);
            user = users.save(user);

            MvcResult asyncResult = mockMvc.perform(post("/api/ai/chat/stream")
                            .header("Authorization", "Bearer " + jwtService.generateToken(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"hello\",\"language\":\"en\"}"))
                    .andExpect(request().asyncStarted())
                    .andReturn();
            mockMvc.perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("\"type\":\"done\"")));
        }
    }

    private void stubStream() {
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("Local answer");
            return null;
        }).when(ollamaClient).stream(anyList(), any());
    }

    private User user(String phone) {
        User user = new User();
        user.setFirstName("AI");
        user.setLastName("Test");
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        return users.save(user);
    }
}
