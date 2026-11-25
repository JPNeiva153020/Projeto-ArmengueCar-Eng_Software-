package br.com.armenguecar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:integration_test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Transactional
class IntegratedFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email, "password", "1234"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).get("token").asText();
    }

    @Test
    void staffCreatesClientAndOrderAndBudgetRulesAreEnforced() throws Exception {
        String auth = login("cristina@armenguecar.local");
        String client = mvc.perform(post("/api/clients").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente Teste\",\"phone\":\"12345\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Cliente Teste"))
                .andReturn().getResponse().getContentAsString();
        String clientId = json.readTree(client).get("id").asText();
        String payload = json.writeValueAsString(java.util.Map.of("clientId", clientId, "plate", "TST-1A23", "vehicle", "Carro teste", "problem", "Reparo"));
        String created = mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("RECEPCAO_CHECKIN"))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(created).get("id").asText();
        mvc.perform(patch("/api/orders/{id}/advance", id).header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("DIAGNOSTICO"));
        mvc.perform(patch("/api/orders/{id}/advance", id).header("Authorization", auth)).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/orders/{id}/budget", id).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":200}")).andExpect(status().isOk());
        mvc.perform(patch("/api/orders/{id}/advance", id).header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("AGUARDANDO_APROVACAO"));
        mvc.perform(patch("/api/orders/{id}/decision", id).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("AGUARDANDO_INICIO"));
        mvc.perform(get("/api/orders/{id}", id).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    void clientCannotReadOtherClientsOrOrdersOrModifyStock() throws Exception {
        String auth = login("eduardo@armenguecar.local");
        mvc.perform(get("/api/orders").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/clients").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(post("/api/stock/movements").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemName\":\"Teste\",\"unit\":\"un\",\"quantity\":1,\"type\":\"ENTRADA\"}")).andExpect(status().isForbidden());
    }

    @Test
    void stockMovementIsPersisted() throws Exception {
        String auth = login("cristina@armenguecar.local");
        mvc.perform(post("/api/stock/movements").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Peça teste integração\",\"unit\":\"un\",\"quantity\":3,\"type\":\"ENTRADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.item.quantity").value(3));
        mvc.perform(get("/api/stock").header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Peça teste integração')].quantity").value(3.0));
    }
}
