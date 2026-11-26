package br.com.armenguecar;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:budget_test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("dev") @AutoConfigureMockMvc @Transactional
class BudgetFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired StockItemRepository stock;
    private String login(String email, String password) throws Exception {
        var r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email",email,"password",password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer "+json.readTree(r).get("token").asText();
    }
    private String postJson(String path, String auth, Object body) throws Exception {
        return mvc.perform(post(path).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
    private record Fixture(String staff, String client, String orderId, String budgetId) {}
    private Fixture setup(int partQuantity) throws Exception {
        String admin=login("paula@armenguecar.local","1234"), staff=login("cristina@armenguecar.local","1234");
        var account=json.readTree(postJson("/api/users",admin,Map.of("name","Cliente Orçamento","email","orcamento@demo.local",
                "password","senha-123","role","CLIENTE")));
        var client=json.readTree(postJson("/api/clients",staff,Map.of("name","Cliente Orçamento","phone","1234")));
        mvc.perform(patch("/api/clients/{id}/user",client.get("id").asText()).header("Authorization",admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("userId",account.get("id").asText()))))
                .andExpect(status().isOk());
        var car=json.readTree(postJson("/api/vehicles",staff,Map.of("clientId",client.get("id").asText(),"plate","TST-9Z99","model","Carro")));
        var order=json.readTree(postJson("/api/orders",staff,Map.of("vehicleId",car.get("id").asText(),"problem","Reparo")));
        String id=order.get("id").asText();
        mvc.perform(patch("/api/orders/{id}/advance",id).header("Authorization",staff)).andExpect(status().isOk());
        mvc.perform(put("/api/orders/{id}/diagnosis",id).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON)
                .content("{\"damages\":\"Avaria\",\"symptoms\":\"Ruído\",\"completed\":true}")).andExpect(status().isOk());
        var piece=stock.findByNameIgnoreCase("Tinta PU Branco").orElseThrow();
        postJson("/api/orders/"+id+"/planned-items",staff,Map.of("itemType","PECA","stockItemId",piece.getId(),"quantity",partQuantity,"unitPrice",10));
        postJson("/api/orders/"+id+"/planned-items",staff,Map.of("itemType","MAO_DE_OBRA","description","Pintura","quantity",1,"unitPrice",100));
        var budget=json.readTree(postJson("/api/orders/"+id+"/budgets",staff,Map.of()));
        return new Fixture(staff,login("orcamento@demo.local","senha-123"),id,budget.get("id").asText());
    }
    @Test
    void approvalReservesAvailableStockOnlyOnceAndRetryCompletesAfterReplenishment() throws Exception {
        Fixture f=setup(25);
        var piece=stock.findByNameIgnoreCase("Tinta PU Branco").orElseThrow();
        String decision="{\"decision\":\"APROVAR\"}";
        String path="/api/portal/orders/"+f.orderId+"/budgets/"+f.budgetId+"/decision";
        mvc.perform(post(path).header("Authorization",f.client).contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.total").value(350.0));
        mvc.perform(post(path).header("Authorization",f.client).contentType(MediaType.APPLICATION_JSON).content(decision))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/{id}",f.orderId).header("Authorization",f.staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("AGUARDANDO_PECAS"));
        org.junit.jupiter.api.Assertions.assertEquals(18.0,stock.findById(piece.getId()).orElseThrow().getReservedQuantity());
        mvc.perform(post("/api/stock/movements").header("Authorization",f.staff).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemName\":\"Tinta PU Branco\",\"unit\":\"L\",\"quantity\":1,\"type\":\"SAIDA\"}"))
                .andExpect(status().isBadRequest());
        postJson("/api/stock/movements",f.staff,Map.of("itemName","Tinta PU Branco","unit","L","quantity",10,"type","ENTRADA"));
        postJson("/api/orders/"+f.orderId+"/budgets/retry-reservation",f.staff,Map.of());
        mvc.perform(get("/api/orders/{id}",f.orderId).header("Authorization",f.staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("AGUARDANDO_INICIO"));
        org.junit.jupiter.api.Assertions.assertEquals(25.0,stock.findById(piece.getId()).orElseThrow().getReservedQuantity());
        mvc.perform(patch("/api/orders/{id}/advance",f.orderId).header("Authorization",f.staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("EXECUCAO"));
        mvc.perform(patch("/api/orders/{id}/advance",f.orderId).header("Authorization",f.staff)).andExpect(status().isBadRequest());
        var step=json.readTree(postJson("/api/orders/"+f.orderId+"/execution-steps",f.staff,Map.of("kind","PINTURA")));
        String stepPath="/api/orders/"+f.orderId+"/execution-steps/"+step.get("id").asText();
        mvc.perform(post("/api/orders/{id}/execution-steps",f.orderId).header("Authorization",f.staff)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"kind\":\"MONTAGEM\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders/{id}/execution-steps",f.orderId).header("Authorization",f.client))
                .andExpect(status().isForbidden());

        postJson(stepPath+"/pause",f.staff,Map.of());
        mvc.perform(post(stepPath+"/pause").header("Authorization",f.staff)).andExpect(status().isBadRequest());
        postJson(stepPath+"/resume",f.staff,Map.of());
        postJson(stepPath+"/finish",f.staff,Map.of());
        mvc.perform(patch("/api/orders/{id}/advance",f.orderId).header("Authorization",f.staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("CONTROLE_QUALIDADE"));
        mvc.perform(post("/api/orders/{id}/rework",f.orderId).header("Authorization",f.staff)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Refazer pintura\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("EXECUCAO"));
        mvc.perform(post("/api/orders/{id}/rework",f.orderId).header("Authorization",f.staff)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Duplicado\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/orders/{id}/advance",f.orderId).header("Authorization",f.staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("CONTROLE_QUALIDADE"));
        mvc.perform(patch("/api/orders/{id}/checklist",f.orderId).header("Authorization",f.staff)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"checklist\":[true,true,true,true,true]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("PRONTO_ENTREGA"));
        mvc.perform(get("/api/orders/{id}/delivery",f.orderId).header("Authorization",f.staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reserved").value(25.0));
        String delivery=json.writeValueAsString(Map.of("items",java.util.List.of(Map.of("itemId",piece.getId(),"usedQuantity",23))));
        mvc.perform(post("/api/orders/{id}/delivery",f.orderId).header("Authorization",f.staff)
                        .contentType(MediaType.APPLICATION_JSON).content(delivery))
                .andExpect(status().isOk()).andExpect(jsonPath("$.closedReason").value("ENTREGA"));
        mvc.perform(post("/api/orders/{id}/delivery",f.orderId).header("Authorization",f.staff)
                        .contentType(MediaType.APPLICATION_JSON).content(delivery))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(5.0,stock.findById(piece.getId()).orElseThrow().getQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(0.0,stock.findById(piece.getId()).orElseThrow().getReservedQuantity());
    }
    @Test
    void changeRequestKeepsPreviousVersionAndRequiresNewDecision() throws Exception {
        Fixture f=setup(1);
        String path="/api/portal/orders/"+f.orderId+"/budgets/"+f.budgetId+"/decision";
        mvc.perform(post(path).header("Authorization",f.client).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"SOLICITAR_ALTERACAO\"}")).andExpect(status().isOk());
        postJson("/api/orders/"+f.orderId+"/planned-items",f.staff,
                Map.of("itemType","MAO_DE_OBRA","description","Polimento","quantity",1,"unitPrice",50));
        var next=json.readTree(postJson("/api/orders/"+f.orderId+"/budgets",f.staff,Map.of()));
        org.junit.jupiter.api.Assertions.assertEquals(2,next.get("version").asInt());
        mvc.perform(post(path).header("Authorization",f.client).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APROVAR\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/portal/orders/{id}/budgets",f.orderId).header("Authorization",f.client))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
    @Test
    void unrelatedClientCannotReadOrDecideAndRefusalClosesWithoutReserving() throws Exception {
        Fixture f=setup(1);
        String other=login("eduardo@armenguecar.local","1234");
        String base="/api/portal/orders/"+f.orderId+"/budgets";
        mvc.perform(get(base).header("Authorization",other)).andExpect(status().isNotFound());
        mvc.perform(post(base+"/"+f.budgetId+"/decision").header("Authorization",other)
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROVAR\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(base+"/"+f.budgetId+"/decision").header("Authorization",f.client)
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"RECUSAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECUSADO"));
        mvc.perform(get("/api/orders/{id}",f.orderId).header("Authorization",f.staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.closedReason").value("RECUSA"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(0.0,
                stock.findByNameIgnoreCase("Tinta PU Branco").orElseThrow().getReservedQuantity());
    }
}
