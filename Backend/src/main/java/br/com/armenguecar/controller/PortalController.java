package br.com.armenguecar.controller;

import br.com.armenguecar.entity.Client;
import br.com.armenguecar.entity.ServiceOrder;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.repository.ClientRepository;
import br.com.armenguecar.repository.ServiceOrderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal")
@PreAuthorize("hasRole('CLIENTE')")
public class PortalController {
    private final ClientRepository clients;
    private final ServiceOrderRepository orders;
    public PortalController(ClientRepository clients, ServiceOrderRepository orders) {
        this.clients = clients; this.orders = orders;
    }
    @GetMapping("/orders")
    public List<PortalOrder> orders(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Client client = clients.findByUserId(user.getId()).orElse(null);
        if (client == null) return List.of();
        return orders.findByClientId(client.getId()).stream().map(PortalOrder::from).toList();
    }
    public record PortalOrder(UUID id, String number, String plate, String vehicle,
                              String stage, int progress, String problem) {
        static PortalOrder from(ServiceOrder order) {
            return new PortalOrder(order.getId(), order.getNumber(), order.getPlate(), order.getVehicle(),
                    order.getStage().name(), order.getProgress(), order.getProblem());
        }
    }
}
