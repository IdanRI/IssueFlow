package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.Role;
import com.att.tdp.issueflow.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private TicketDependencyRepository ticketDependencyRepository;
    @Autowired private CommentMentionRepository commentMentionRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;
    private Long userId;
    private Long projectId;

    @BeforeEach
    void setup() throws Exception {
        commentMentionRepository.deleteAll();
        commentRepository.deleteAll();
        ticketDependencyRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("dev1").email("dev1@test.com").fullName("Dev One")
                .password(passwordEncoder.encode("pass123")).role(Role.DEVELOPER).build());
        userId = user.getId();

        Project project = projectRepository.save(Project.builder()
                .name("Test Project").description("Desc").ownerId(userId).build());
        projectId = project.getId();

        token = login("dev1", "pass123");
    }

    @Test
    void createAndGetTicket() throws Exception {
        String body = """
                {"title":"Bug","description":"Fix it","status":"TODO","priority":"HIGH",
                 "type":"BUG","projectId":%d,"assigneeId":%d}
                """.formatted(projectId, userId);

        MvcResult result = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bug"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long ticketId = json.get("id").asLong();

        mockMvc.perform(get("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));
    }

    @Test
    void updateTicket_statusTransition() throws Exception {
        Long ticketId = createTicket("Task", "TODO", "LOW", "FEATURE");

        mockMvc.perform(patch("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateTicket_backwardTransition_fails() throws Exception {
        Long ticketId = createTicket("Task", "TODO", "LOW", "FEATURE");

        mockMvc.perform(patch("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TODO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTicketsByProject() throws Exception {
        createTicket("T1", "TODO", "LOW", "BUG");
        createTicket("T2", "TODO", "HIGH", "FEATURE");

        mockMvc.perform(get("/tickets?projectId=" + projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void softDeleteAndRestore() throws Exception {
        Long ticketId = createTicket("DeleteMe", "TODO", "LOW", "BUG");

        mockMvc.perform(delete("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // Need admin for restore
        User admin = userRepository.save(User.builder()
                .username("admin1").email("admin@test.com").fullName("Admin")
                .password(passwordEncoder.encode("admin123")).role(Role.ADMIN).build());
        String adminToken = login("admin1", "admin123");

        mockMvc.perform(post("/tickets/" + ticketId + "/restore")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("DeleteMe"));
    }

    private Long createTicket(String title, String status, String priority, String type) throws Exception {
        String body = """
                {"title":"%s","description":"desc","status":"%s","priority":"%s",
                 "type":"%s","projectId":%d,"assigneeId":%d}
                """.formatted(title, status, priority, type, projectId, userId);

        MvcResult result = mockMvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
