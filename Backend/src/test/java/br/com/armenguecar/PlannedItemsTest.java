package br.com.armenguecar;

import br.com.armenguecar.repository.ServiceOrderRepository;
import br.com.armenguecar.repository.StockItemRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:planned_test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("dev") @AutoConfigureMockMvc @Transactional
class PlannedItemsTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ServiceOrderRepository orders;
    @Autowired StockItemRepository stock;
    private String login(String email) throws Exception {
        String body=json.writeValueAsString(Map.of("email",email,"password","1234"));
        var response=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer "+json.readTree(response).get("token").asText();
    }
    @Test
    void plannedPartsAndLaborDoNotChangeStockAndAreRestrictedToDiagnosis() throws Exception {
        String auth=login("cristina@armenguecar.local"), client=login("eduardo@armenguecar.local");
        var order=orders.findAll().stream().filter(o -> o.getNumber().equals("OS-1051")).findFirst().orElseThrow();
        var another=orders.findAll().stream().filter(o -> o.getNumber().equals("OS-1050")).findFirst().orElseThrow();
        var piece=stock.findAll().get(0); double quantityBefore=piece.getQuantity();
        String part=json.writeValueAsString(Map.of("itemType","PECA","stockItemId",piece.getId(),
                "quantity",2,"unitPrice",10));
        String labor=json.writeValueAsString(Map.of("itemType","MAO_DE_OBRA","description","Pintura",
                "quantity",1,"unitPrice",100));
        var result=mvc.perform(post("/api/orders/{id}/planned-items",order.getId()).header("Authorization",auth)
                        .contentType(MediaType.APPLICATION_JSON).content(part))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemType").value("PECA"))
                .andReturn().getResponse().getContentAsString();
        String itemId=json.readTree(result).get("id").asText();
        mvc.perform(post("/api/orders/{id}/planned-items",order.getId()).header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content(part)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders/{id}/planned-items",order.getId()).header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content(labor)).andExpect(status().isOk());
        mvc.perform(get("/api/orders/{id}/planned-items",order.getId()).header("Authorization",client))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/orders/{id}/planned-items",another.getId()).header("Authorization",auth)
                .contentType(MediaType.APPLICATION_JSON).content(labor)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/orders/{id}/planned-items/{itemId}",order.getId(),itemId).header("Authorization",auth))
                .andExpect(status().isNoContent());
        assertEquals(quantityBefore, stock.findById(piece.getId()).orElseThrow().getQuantity());
    }
}
