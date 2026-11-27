package br.com.armenguecar;
import br.com.armenguecar.repository.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:registration_restock;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("dev") @AutoConfigureMockMvc @Transactional
class RegistrationRestockTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired ClientRepository clients; @Autowired UserRepository users; @Autowired StockItemRepository stock; @Autowired StockMovementRepository movements;
 String login(String email) throws Exception {var r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",email,"password","1234")))).andExpect(status().isOk()).andReturn();return "Bearer "+json.readTree(r.getResponse().getContentAsString()).get("token").asText();}
 @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED) void unifiedRegistrationIsAtomicAndAdminOnly() throws Exception {
  String admin=login("paula@armenguecar.local"), manager=login("cristina@armenguecar.local");
  var payload=Map.of("name","Cliente integrado","phone","123456","email","unificado@demo.local","password","senha-123","plate","ABC7D88","vehicle","Teste");
  mvc.perform(post("/api/clients/with-account").header("Authorization",manager).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isForbidden());
  var response=mvc.perform(post("/api/clients/with-account").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isOk()).andExpect(jsonPath("$.userId").exists()).andReturn();
  var id=json.readTree(response.getResponse().getContentAsString()).get("userId").asText();
  assertTrue(users.findByEmailIgnoreCase("unificado@demo.local").isPresent());
  assertTrue(clients.findByUserId(java.util.UUID.fromString(id)).isPresent());
  long n=clients.count(),u=users.count();
  mvc.perform(post("/api/clients/with-account").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isBadRequest());
  assertEquals(n,clients.count());assertEquals(u,users.count());
  var duplicatePlate=Map.of("name","Falha","phone","123456","email","rollback@demo.local","password","senha-123","plate","ABC7D88","vehicle","Outro");
  mvc.perform(post("/api/clients/with-account").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(duplicatePlate))).andExpect(status().isBadRequest());
  assertTrue(users.findByEmailIgnoreCase("rollback@demo.local").isEmpty());
 }
 @Test void restockReceiptIsAuthorizedAtomicAndIdempotent() throws Exception {
  String manager=login("cristina@armenguecar.local"),mechanic=login("marcos@armenguecar.local");
  var item=stock.findByNameIgnoreCase("Tinta PU Branco").orElseThrow();double before=item.getQuantity();long beforeMovements=movements.count();
  var payload=Map.of("itemId",item.getId(),"quantity",3,"note","Teste");
  mvc.perform(post("/api/stock/restock-requests").header("Authorization",mechanic).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isForbidden());
  var created=mvc.perform(post("/api/stock/restock-requests").header("Authorization",manager).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA")).andReturn();
  String id=json.readTree(created.getResponse().getContentAsString()).get("id").asText();
  assertEquals(before,stock.findById(item.getId()).orElseThrow().getQuantity());
  mvc.perform(post("/api/stock/restock-requests/{id}/receive",id).header("Authorization",manager)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECEBIDA"));
  mvc.perform(post("/api/stock/restock-requests/{id}/receive",id).header("Authorization",manager)).andExpect(status().isOk());
  assertEquals(before+3,stock.findById(item.getId()).orElseThrow().getQuantity());assertEquals(beforeMovements+1,movements.count());
  mvc.perform(post("/api/stock/restock-requests/{id}/cancel",id).header("Authorization",manager)).andExpect(status().isBadRequest());
  var other=mvc.perform(post("/api/stock/restock-requests").header("Authorization",manager).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isOk()).andReturn();
  String cancelled=json.readTree(other.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(post("/api/stock/restock-requests/{id}/cancel",cancelled).header("Authorization",manager)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
  mvc.perform(post("/api/stock/restock-requests/{id}/receive",cancelled).header("Authorization",manager)).andExpect(status().isBadRequest());
  assertEquals(before+3,stock.findById(item.getId()).orElseThrow().getQuantity());

 }
}
