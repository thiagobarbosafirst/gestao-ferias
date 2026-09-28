package com.taskflow.vacations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração ponta a ponta: Spring Boot completo + PostgreSQL real num container (Testcontainers).
 * Usa os dados de exemplo do DataSeeder. Se o Docker não estiver disponível o teste é ignorado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class VacationApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    void requestsWithoutTokenGet401() throws Exception {
        mvc.perform(get("/api/vacations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void wrongPasswordGets401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@taskflow.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminManagesUsers() throws Exception {
        mvc.perform(get("/api/users").header("Authorization", bearer("admin@taskflow.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber());

        mvc.perform(get("/api/users").header("Authorization", bearer("marco@taskflow.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidBodyGets400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/vacations").header("Authorization", bearer("joao@taskflow.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void overlappingRequestGets409AndAdjacentIsAccepted() throws Exception {
        LocalDate start = LocalDate.now().plusYears(1);
        String token = bearer("joao@taskflow.com");

        // João marca 5 dias
        mvc.perform(post("/api/vacations").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(start, start.plusDays(4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.days").value(5))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        // Pedro tenta começar no último dia de João -> sobreposição (datas inclusivas)
        mvc.perform(post("/api/vacations").header("Authorization", bearer("pedro@taskflow.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(start.plusDays(4), start.plusDays(8))))
                .andExpect(status().isConflict());

        // Começar no dia seguinte já é permitido
        mvc.perform(post("/api/vacations").header("Authorization", bearer("pedro@taskflow.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(start.plusDays(5), start.plusDays(8))))
                .andExpect(status().isCreated());
    }

    private String body(LocalDate start, LocalDate end) {
        return "{\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\"}";
    }

    /** Faz login com a password dos dados de exemplo e devolve "Bearer <token>". */
    private String bearer(String email) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(json);
        return "Bearer " + node.get("token").asText();
    }
}
