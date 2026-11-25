package br.com.armenguecar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:armenguecar_test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class ApplicationSmokeTest {
    @Autowired MockMvc mvc;

    @Test
    void publicHealthAndDemoLoginWorkButOrdersRequireAuthentication() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/orders")).andExpect(status().is4xxClientError());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cristina@armenguecar.local\",\"password\":\"1234\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
}
