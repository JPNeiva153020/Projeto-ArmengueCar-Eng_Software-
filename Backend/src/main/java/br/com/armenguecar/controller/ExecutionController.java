package br.com.armenguecar.controller;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.ExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/orders/{orderId}/execution-steps")
@PreAuthorize("hasAnyRole('GERENTE','MECANICO','ADMIN')")
public class ExecutionController {
 private final ExecutionService service;
 public ExecutionController(ExecutionService service){this.service=service;}
 public record StartRequest(@NotBlank String kind) {}
 private UUID actor(Authentication a){return ((User)a.getPrincipal()).getId();}
 @GetMapping public List<ExecutionService.StepView> list(@PathVariable UUID orderId){return service.list(orderId);}
 @PostMapping public ExecutionService.StepView start(@PathVariable UUID orderId,@Valid @RequestBody StartRequest r,Authentication a){return service.start(orderId,r.kind(),actor(a));}
 @PostMapping("/{stepId}/{action}") public ExecutionService.StepView transition(@PathVariable UUID orderId,@PathVariable UUID stepId,@PathVariable String action,Authentication a){return service.transition(orderId,stepId,action,actor(a));}
}
