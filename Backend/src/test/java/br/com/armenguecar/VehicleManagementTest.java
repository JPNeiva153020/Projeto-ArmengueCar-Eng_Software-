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
        "spring.datasource.url=jdbc:h2:mem:vehicle_test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Transactional
class VehicleManagementTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "1234"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).get("token").asText();
    }
    @Test
    void vehiclesHaveOwnersAndDuplicatePlatesAreRejected() throws Exception {
        String manager=login("cristina@armenguecar.local");
        String client=login("eduardo@armenguecar.local");
        var created=mvc.perform(post("/api/clients").header("Authorization",manager).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente veículo\",\"phone\":\"1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String clientId=json.readTree(created).get("id").asText();
        String payload=json.writeValueAsString(Map.of("clientId",clientId,"plate"," tst-2b34 ",
                "model","Teste", "brand","Marca", "modelYear",2020));
        var response=mvc.perform(post("/api/vehicles").header("Authorization",manager)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plate").value("TST-2B34"))
                .andExpect(jsonPath("$.client.id").value(clientId))
                .andReturn().getResponse().getContentAsString();
        String id=json.readTree(response).get("id").asText();
        mvc.perform(post("/api/vehicles").header("Authorization",manager).contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/vehicles/{id}",id).header("Authorization",manager).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("clientId",clientId,"plate","TST-2B34","model","Atualizado"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.model").value("Atualizado"));
        mvc.perform(get("/api/vehicles").header("Authorization",client)).andExpect(status().isForbidden());
    }
}
