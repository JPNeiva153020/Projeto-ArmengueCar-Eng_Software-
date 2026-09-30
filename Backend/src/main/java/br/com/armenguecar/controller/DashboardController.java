package br.com.armenguecar.controller;

import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.repository.ServiceOrderRepository;
import br.com.armenguecar.repository.StockItemRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ServiceOrderRepository orderRepository;
    private final StockItemRepository stockItemRepository;

    public DashboardController(
            ServiceOrderRepository orderRepository,
            StockItemRepository stockItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.stockItemRepository = stockItemRepository;
    }

    @GetMapping
    public Map<String, Object> summary() {
        var orders = orderRepository.findAll();

        long activeOrders = orders.stream()
                .filter(order -> order.getStage() != OrderStage.PRONTO_ENTREGA)
                .count();

        double totalBudget = orders.stream()
                .mapToDouble(order -> order.getValue())
                .sum();

        long criticalStock = stockItemRepository.findAll().stream()
                .filter(item -> item.getQuantity() <= item.getReorderPoint())
                .count();

        Map<String, Long> byStage = new LinkedHashMap<>();
        Arrays.stream(OrderStage.values()).forEach(stage ->
                byStage.put(
                        stage.name(),
                        orders.stream().filter(order -> order.getStage() == stage).count()
                )
        );

        return Map.of(
                "totalOrders", orders.size(),
                "activeOrders", activeOrders,
                "totalBudget", totalBudget,
                "criticalStock", criticalStock,
                "byStage", byStage
        );
    }
}
