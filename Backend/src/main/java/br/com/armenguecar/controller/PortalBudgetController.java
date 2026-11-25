package br.com.armenguecar.controller;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.BudgetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/portal/orders/{orderId}/budgets")
@PreAuthorize("hasRole('CLIENTE')")
public class PortalBudgetController {
    private final BudgetService budgets;
    public PortalBudgetController(BudgetService budgets) { this.budgets=budgets; }
    private UUID actor(Authentication auth) { return ((User) auth.getPrincipal()).getId(); }
    public record Decision(@NotBlank String decision) {}
    @GetMapping public List<BudgetService.BudgetView> list(@PathVariable UUID orderId, Authentication auth) {
        return budgets.clientList(orderId,actor(auth));
    }
    @PostMapping("/{budgetId}/decision")
    public BudgetService.BudgetView decide(@PathVariable UUID orderId, @PathVariable UUID budgetId,
                                            @Valid @RequestBody Decision decision, Authentication auth) {
        return budgets.decide(orderId,budgetId,actor(auth),decision.decision());
    }
}
