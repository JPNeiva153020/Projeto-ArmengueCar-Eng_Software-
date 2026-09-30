package br.com.armenguecar.controller;

import br.com.armenguecar.entity.ServiceOrder;
import br.com.armenguecar.service.OrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class PhotoController {

    private final OrderService orderService;

    public PhotoController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{id}/photos")
    public PhotoResponse addPhoto(@PathVariable UUID id) {
        ServiceOrder order = orderService.addPhoto(id);
        return new PhotoResponse(order.getId(), order.getPhotos());
    }

    public record PhotoResponse(UUID orderId, int photos) {
    }
}
