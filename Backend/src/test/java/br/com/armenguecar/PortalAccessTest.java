package br.com.armenguecar;

import br.com.armenguecar.repository.ClientRepository;
import br.com.armenguecar.repository.UserRepository;
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
        "spring.datasource.url=jdbc:h2:mem:portal_test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Transactional
class PortalAccessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClientRepository clients;
    @Autowired UserRepository users;
    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "1234"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).get("token").asText();
    }
    @Test
    void adminLinksClientAndPortalShowsOnlyOwnersOrders() throws Exception {
        String admin = login("paula@armenguecar.local");
        String manager = login("cristina@armenguecar.local");
        String client = login("eduardo@armenguecar.local");
        var owner = clients.findByEmailIgnoreCase("eduardo@email.demo").orElseThrow();
        var other = clients.findByEmailIgnoreCase("ana@email.demo").orElseThrow();
        var account = users.findByEmailIgnoreCase("eduardo@armenguecar.local").orElseThrow();
        String body = json.writeValueAsString(Map.of("userId", account.getId()));
        mvc.perform(get("/api/portal/orders").header("Authorization", client))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(patch("/api/clients/{id}/user", owner.getId()).header("Authorization", manager)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/clients/{id}/user", owner.getId()).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(patch("/api/clients/{id}/user", other.getId()).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/portal/orders").header("Authorization", client)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("OS-1048"))
                .andExpect(jsonPath("$[?(@.number == 'OS-1049')]").isEmpty())
                .andExpect(jsonPath("$[0].client").doesNotExist());
        mvc.perform(get("/api/portal/orders").header("Authorization", manager)).andExpect(status().isForbidden());
    }
}
