package br.com.armenguecar.controller;
import br.com.armenguecar.entity.ServiceOrder;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/orders/{orderId}/delivery")
@PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
public class DeliveryController {
    private final DeliveryService delivery;
    public DeliveryController(DeliveryService delivery) { this.delivery=delivery; }
    @GetMapping public List<DeliveryService.ReservationView> list(@PathVariable UUID orderId) { return delivery.list(orderId); }
    @PostMapping public ServiceOrder deliver(@PathVariable UUID orderId, @Valid @RequestBody DeliveryService.DeliveryRequest request, Authentication auth) {
        return delivery.deliver(orderId,request,((User)auth.getPrincipal()).getId());
    }
}
