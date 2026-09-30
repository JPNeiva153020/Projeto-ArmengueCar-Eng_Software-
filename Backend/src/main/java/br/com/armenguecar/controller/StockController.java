package br.com.armenguecar.controller;

import br.com.armenguecar.dto.StockDtos.MovementRequest;
import br.com.armenguecar.dto.StockDtos.UpdateStockRequest;
import br.com.armenguecar.entity.StockItem;
import br.com.armenguecar.entity.StockMovement;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.StockService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public List<StockItem> list() {
        return stockService.list();
    }

    @PostMapping("/movements")
    public StockMovement createMovement(
            @Valid @RequestBody MovementRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return stockService.createMovement(request, user.getId());
    }

    @PutMapping("/{id}")
    public StockItem update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        return stockService.update(id, request);
    }
}
