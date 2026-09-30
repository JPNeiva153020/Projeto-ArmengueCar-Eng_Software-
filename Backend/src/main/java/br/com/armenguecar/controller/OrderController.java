package br.com.armenguecar.controller;

import br.com.armenguecar.dto.OrderDtos.ChecklistRequest;
import br.com.armenguecar.dto.OrderDtos.CreateOrderRequest;
import br.com.armenguecar.dto.OrderDtos.DecisionRequest;
import br.com.armenguecar.dto.OrderDtos.UpdateBudgetRequest;
import br.com.armenguecar.entity.ServiceOrder;
import br.com.armenguecar.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<ServiceOrder> list() {
        return orderService.list();
    }

    @GetMapping("/{id}")
    public ServiceOrder get(@PathVariable UUID id) {
        return orderService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'MECANICO', 'ADMIN')")
    public ServiceOrder create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @PatchMapping("/{id}/advance")
    public ServiceOrder advance(@PathVariable UUID id) {
        return orderService.advance(id);
    }

    @PatchMapping("/{id}/retreat")
    public ServiceOrder retreat(@PathVariable UUID id) {
        return orderService.retreat(id);
    }

    @PatchMapping("/{id}/budget")
    public ServiceOrder updateBudget(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        return orderService.updateBudget(id, request);
    }

    @PatchMapping("/{id}/decision")
    public ServiceOrder decide(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request
    ) {
        return orderService.decide(id, request);
    }

    @PatchMapping("/{id}/checklist")
    public ServiceOrder updateChecklist(
            @PathVariable UUID id,
            @Valid @RequestBody ChecklistRequest request
    ) {
        return orderService.updateChecklist(id, request);
    }

    @PostMapping("/{id}/deliver")
    public ServiceOrder deliver(@PathVariable UUID id) {
        return orderService.deliver(id);
    }

    @PostMapping("/{id}/photos")
    public PhotoResponse addPhoto(@PathVariable UUID id) {
        ServiceOrder order = orderService.addPhoto(id);
        return new PhotoResponse(order.getId(), order.getPhotos());
    }

    public record PhotoResponse(UUID orderId, int photos) {
    }
}
