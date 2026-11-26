package br.com.armenguecar.service;

import br.com.armenguecar.entity.*;
import br.com.armenguecar.enums.MovementType;
import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeliveryService {
    private final ServiceOrderRepository orders;
    private final BudgetRepository budgets;
    private final BudgetLineRepository lines;
    private final PartReservationRepository reservations;
    private final StockItemRepository stock;
    private final StockMovementRepository movements;
    private final UserRepository users;
    private final AuditService audit;
    private final NotificationOutboxService outbox;
    public DeliveryService(ServiceOrderRepository orders, BudgetRepository budgets, BudgetLineRepository lines,
                           PartReservationRepository reservations, StockItemRepository stock,
                           StockMovementRepository movements, UserRepository users, AuditService audit, NotificationOutboxService outbox) {
        this.orders=orders; this.budgets=budgets; this.lines=lines; this.reservations=reservations;
        this.stock=stock; this.movements=movements; this.users=users; this.audit=audit; this.outbox=outbox;
    }
    public record ItemUsed(@NotNull UUID itemId,
                           @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal usedQuantity) {}
    public record DeliveryRequest(@NotNull List<@NotNull @Valid ItemUsed> items) {}
    public record ReservationView(UUID itemId, String name, String unit, BigDecimal reserved) {}
    @Transactional(readOnly=true)
    public List<ReservationView> list(UUID orderId) {
        ServiceOrder order=orders.findById(orderId).orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt()!=null) throw new IllegalStateException("OS já encerrada");
        Budget b=approvedBudget(orderId);
        return reservations.findByBudgetId(b.getId()).stream()
                .map(r -> new ReservationView(r.getStockItem().getId(),r.getStockItem().getName(),
                        r.getStockItem().getUnit(),r.getQuantity())).toList();
    }
    @Transactional
    public ServiceOrder deliver(UUID orderId, DeliveryRequest request, UUID actorId) {
        ServiceOrder order=orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt()!=null) {
            if ("ENTREGA".equals(order.getClosedReason())) return order;
            throw new IllegalStateException("OS encerrada sem entrega");
        }
        if (order.getStage()!=OrderStage.PRONTO_ENTREGA
                || !"[true, true, true, true, true]".equals(order.getChecklist()))
            throw new IllegalStateException("Entrega exige checklist aprovado");
        if (!order.isApproved() || !order.isReserved())
            throw new IllegalStateException("Aprovação e reservas completas são necessárias");
        Budget budget=approvedBudget(orderId);
        List<PartReservation> rs=reservations.findByBudgetId(budget.getId()).stream()
                .sorted(Comparator.comparing(r -> r.getStockItem().getId())).toList();
        Map<UUID,BigDecimal> supplied=new HashMap<>();
        for (ItemUsed item:request.items())
            if (supplied.put(item.itemId(),item.usedQuantity())!=null)
                throw new IllegalArgumentException("Peça informada mais de uma vez");
        Map<UUID,BigDecimal> required=lines.findByBudgetId(budget.getId()).stream()
                .filter(l -> l.getStockItem()!=null)
                .collect(Collectors.toMap(l -> l.getStockItem().getId(),BudgetLine::getQuantity,BigDecimal::add));
        if (supplied.size()!=rs.size() || rs.size()!=required.size())
            throw new IllegalArgumentException("Informe a quantidade utilizada de cada peça reservada");
        User actor=users.findById(actorId).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        for (PartReservation reservation:rs) {
            UUID itemId=reservation.getStockItem().getId();
            BigDecimal used=supplied.get(itemId), reserved=reservation.getQuantity();
            if (used==null || !required.containsKey(itemId) || reserved.compareTo(required.get(itemId))!=0
                    || used.compareTo(reserved)>0)
                throw new IllegalArgumentException("Quantidades informadas não correspondem às reservas");
            StockItem item=stock.findByIdForUpdate(itemId).orElseThrow();
            BigDecimal physical=BigDecimal.valueOf(item.getQuantity());
            BigDecimal totalReserved=BigDecimal.valueOf(item.getReservedQuantity());
            if (physical.compareTo(used)<0 || totalReserved.compareTo(reserved)<0)
                throw new IllegalStateException("Saldo ou reserva inconsistente; revise o estoque");
            item.setQuantity(physical.subtract(used).doubleValue());
            item.setReservedQuantity(totalReserved.subtract(reserved).doubleValue());
            stock.save(item);
            reservation.setUsedQuantity(used); reservation.setReleasedQuantity(reserved.subtract(used));
            reservations.save(reservation);
            if (used.signum()>0) {
                StockMovement movement=new StockMovement(); movement.setItem(item); movement.setType(MovementType.SAIDA);
                movement.setQuantity(used.doubleValue()); movement.setReason("Entrega "+order.getNumber());
                movement.setCreatedBy(actor); movement.setOrderRef(order); movements.save(movement);
            }
        }
        order.setClosedAt(Instant.now()); order.setClosedReason("ENTREGA"); order.setClosedBy(actorId);
        order.setReserved(false); orders.save(order);
        audit.log(actorId,"ENTREGA_CONFIRMADA",orderId.toString(),order.getNumber());
        outbox.enqueue(order.getClient().getUserId(),orderId,"Entrega confirmada","A entrega da ordem "+order.getNumber()+" foi confirmada.","order");
        return order;
    }
    private Budget approvedBudget(UUID orderId) {
        return budgets.findFirstByOrderIdOrderByVersionDesc(orderId)
                .filter(b -> b.getStatus().equals("APROVADO"))
                .orElseThrow(() -> new IllegalStateException("Orçamento aprovado não encontrado; revise a ordem"));
    }
}
