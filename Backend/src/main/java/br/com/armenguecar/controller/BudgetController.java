package br.com.armenguecar.controller;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.BudgetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/orders/{orderId}/budgets")
public class BudgetController {
    private final BudgetService budgets;
    public BudgetController(BudgetService budgets) { this.budgets=budgets; }
    private UUID actor(Authentication a) { return ((User)a.getPrincipal()).getId(); }
    @GetMapping public List<BudgetService.BudgetView> list(@PathVariable UUID orderId) { return budgets.list(orderId); }
    @PostMapping @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public BudgetService.BudgetView publish(@PathVariable UUID orderId, Authentication auth) {
        return budgets.publish(orderId, actor(auth));
    }
    @PostMapping("/retry-reservation") @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public BudgetService.BudgetView retry(@PathVariable UUID orderId, Authentication auth) {
        return budgets.retryReservation(orderId, actor(auth));
    }
}
