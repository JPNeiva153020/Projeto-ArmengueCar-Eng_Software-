package br.com.armenguecar.controller;

import br.com.armenguecar.entity.PlannedItem;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.repository.PlannedItemRepository;
import br.com.armenguecar.repository.ServiceOrderRepository;
import br.com.armenguecar.repository.StockItemRepository;
import br.com.armenguecar.service.AuditService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/planned-items")
public class PlannedItemController {
    private final PlannedItemRepository items;
    private final ServiceOrderRepository orders;
    private final StockItemRepository stock;
    private final AuditService audit;
    private final br.com.armenguecar.repository.BudgetRepository budgets;
    public PlannedItemController(PlannedItemRepository items, ServiceOrderRepository orders,
                                 StockItemRepository stock, AuditService audit, br.com.armenguecar.repository.BudgetRepository budgets) {
        this.items=items; this.orders=orders; this.stock=stock; this.audit=audit; this.budgets=budgets;
    }
    public record ItemRequest(@NotBlank @Pattern(regexp="PECA|MAO_DE_OBRA") String itemType,
                              UUID stockItemId, @Size(max=255) String description,
                              @NotNull @DecimalMin(value="0.01") @Digits(integer=10, fraction=2) BigDecimal quantity,
                              @NotNull @DecimalMin(value="0.00") @Digits(integer=10, fraction=2) BigDecimal unitPrice) {}
    @GetMapping @Transactional(readOnly=true)
    public List<PlannedItem> list(@PathVariable UUID orderId) {
        if (!orders.existsById(orderId)) throw new EntityNotFoundException("OS não encontrada");
        return items.findByOrderIdOrderByCreatedAtAsc(orderId);
    }
    @PostMapping @Transactional
    public PlannedItem add(@PathVariable UUID orderId, @Valid @RequestBody ItemRequest request, Authentication authentication) {
        var order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt() != null || !(order.getStage() == OrderStage.DIAGNOSTICO
                || (order.getStage() == OrderStage.AGUARDANDO_APROVACAO
                    && budgets.findFirstByOrderIdOrderByVersionDesc(orderId)
                        .map(b -> b.getStatus().equals("ALTERACAO_SOLICITADA")).orElse(false))))
            throw new IllegalStateException("Itens só podem ser alterados no diagnóstico ou após pedido de alteração");
        PlannedItem item = new PlannedItem(); item.setOrder(order); item.setItemType(request.itemType());
        if ("PECA".equals(request.itemType())) {
            if (request.stockItemId() == null) throw new IllegalArgumentException("Selecione uma peça cadastrada");
            if (items.existsByOrderIdAndStockItemId(orderId, request.stockItemId()))
                throw new IllegalArgumentException("Peça já consta nesta ordem");
            var piece = stock.findById(request.stockItemId()).orElseThrow(() -> new EntityNotFoundException("Peça não encontrada"));
            item.setStockItem(piece); item.setDescription(piece.getName());
        } else {
            if (request.stockItemId() != null || request.description() == null || request.description().isBlank())
                throw new IllegalArgumentException("Informe a descrição da mão de obra");
            item.setDescription(request.description().trim());
        }
        User actor = (User) authentication.getPrincipal();
        item.setCreatedBy(actor.getId()); item.setQuantity(request.quantity()); item.setUnitPrice(request.unitPrice());
        PlannedItem saved = items.save(item);
        audit.log(actor.getId(), "ITEM_PREVISTO_CRIADO", orderId.toString(), item.getItemType());
        return saved;
    }
    @DeleteMapping("/{itemId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void remove(@PathVariable UUID orderId, @PathVariable UUID itemId, Authentication authentication) {
        var order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt() != null || !(order.getStage() == OrderStage.DIAGNOSTICO
                || (order.getStage() == OrderStage.AGUARDANDO_APROVACAO
                    && budgets.findFirstByOrderIdOrderByVersionDesc(orderId)
                        .map(b -> b.getStatus().equals("ALTERACAO_SOLICITADA")).orElse(false))))
            throw new IllegalStateException("Itens só podem ser alterados no diagnóstico ou após pedido de alteração");
        var item = items.findById(itemId).filter(i -> i.getOrder().getId().equals(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado"));
        items.delete(item);
        audit.log(((User) authentication.getPrincipal()).getId(), "ITEM_PREVISTO_REMOVIDO", orderId.toString(), null);
    }
}
