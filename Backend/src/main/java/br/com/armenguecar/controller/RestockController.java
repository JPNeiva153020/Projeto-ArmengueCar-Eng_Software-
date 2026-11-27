package br.com.armenguecar.controller;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.RestockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequestMapping("/api/stock/restock-requests") @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
public class RestockController {
 private final RestockService service;
 public RestockController(RestockService service){this.service=service;}
 public record CreateRequest(@NotNull UUID itemId,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal quantity,@Size(max=1000) String note){}
 private UUID actor(Authentication auth){return ((User)auth.getPrincipal()).getId();}
 @GetMapping public Object list(){return service.list();}
 @PostMapping public Object create(@Valid @RequestBody CreateRequest req,Authentication auth){return service.create(req.itemId(),req.quantity(),req.note(),actor(auth));}
 @PostMapping("/{id}/{action}") public Object resolve(@PathVariable UUID id,@PathVariable String action,Authentication auth){return service.resolve(id,action,actor(auth));}
}
