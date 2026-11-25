package br.com.armenguecar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user_test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Transactional
class UserManagementTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String email, String password) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).get("token").asText();
    }

    @Test
    void adminCreatesEditsAndResetsUserAndOldSessionsAreRevoked() throws Exception {
        String admin = login("paula@armenguecar.local", "1234");
        String body = json.writeValueAsString(Map.of("name", "Teste", "email", "teste@demo.local",
                "password", "senha-123", "role", "MECANICO"));
        var response = mvc.perform(post("/api/users").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.authVersion").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).get("id").asText();
        mvc.perform(post("/api/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        String multibytePassword = "á".repeat(40);
        mvc.perform(post("/api/users").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Outro", "email", "outro@demo.local",
                        "password", multibytePassword, "role", "MECANICO"))))
                .andExpect(status().isBadRequest());
        String oldToken = login("teste@demo.local", "senha-123");
        mvc.perform(put("/api/users/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Teste atualizado", "email", "teste@demo.local", "role", "GERENTE"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Teste atualizado"));
        mvc.perform(get("/api/me").header("Authorization", oldToken)).andExpect(status().is4xxClientError());
        String newToken = login("teste@demo.local", "senha-123");
        mvc.perform(patch("/api/users/{id}/password", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"nova-senha-123\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/me").header("Authorization", newToken)).andExpect(status().is4xxClientError());
        login("teste@demo.local", "nova-senha-123");
        mvc.perform(get("/api/audit").header("Authorization", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'SENHA_REDEFINIDA')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.action == 'SENHA_REDEFINIDA')].detail").doesNotExist());
    }

    @Test
    void nonAdminCannotManageAccountsAndAdminCannotDeactivateSelf() throws Exception {
        String manager = login("cristina@armenguecar.local", "1234");
        mvc.perform(get("/api/users").header("Authorization", manager)).andExpect(status().isForbidden());
        String admin = login("paula@armenguecar.local", "1234");
        var me = mvc.perform(get("/api/me").header("Authorization", admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(me).get("id").asText();
        mvc.perform(patch("/api/users/{id}/toggle", id).header("Authorization", admin)).andExpect(status().isBadRequest());
    }
}
