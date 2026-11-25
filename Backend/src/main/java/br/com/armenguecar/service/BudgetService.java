package br.com.armenguecar.service;

import br.com.armenguecar.entity.*;
import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.enums.Role;
import br.com.armenguecar.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class BudgetService {
    private final ServiceOrderRepository orders;
    private final DiagnosisRepository diagnoses;
    private final PlannedItemRepository planned;
    private final BudgetRepository budgets;
    private final BudgetLineRepository lines;
    private final PartReservationRepository reservations;
    private final StockItemRepository stock;
    private final ClientRepository clients;
    private final UserRepository users;
    private final AuditService audit;
    public BudgetService(ServiceOrderRepository orders, DiagnosisRepository diagnoses, PlannedItemRepository planned,
                         BudgetRepository budgets, BudgetLineRepository lines, PartReservationRepository reservations,
                         StockItemRepository stock, ClientRepository clients, UserRepository users, AuditService audit) {
        this.orders=orders; this.diagnoses=diagnoses; this.planned=planned; this.budgets=budgets;
        this.lines=lines; this.reservations=reservations; this.stock=stock; this.clients=clients; this.users=users; this.audit=audit;
    }
    public record LineView(String type, String description, BigDecimal quantity, BigDecimal unitPrice) {}
    public record BudgetView(UUID id, int version, String status, BigDecimal total,
                             Instant publishedAt, Instant decidedAt, List<LineView> items) {}
    private BudgetView view(Budget budget) {
        return new BudgetView(budget.getId(), budget.getVersion(), budget.getStatus(), budget.getTotal(),
                budget.getPublishedAt(), budget.getDecidedAt(), lines.findByBudgetId(budget.getId()).stream()
                .map(x -> new LineView(x.getItemType(), x.getDescription(), x.getQuantity(), x.getUnitPrice())).toList());
    }
    @Transactional(readOnly=true)
    public List<BudgetView> list(UUID orderId) {
        if (!orders.existsById(orderId)) throw new EntityNotFoundException("OS não encontrada");
        return budgets.findByOrderIdOrderByVersionDesc(orderId).stream().map(this::view).toList();
    }
    @Transactional(readOnly=true)
    public List<BudgetView> clientList(UUID orderId, UUID actorId) {
        requireOwner(orderId, actorId);
        return list(orderId);
    }
    @Transactional
    public BudgetView publish(UUID orderId, UUID actorId) {
        ServiceOrder order=lockedOrder(orderId);
        var last=budgets.findFirstByOrderIdOrderByVersionDesc(orderId);
        boolean revision=last.map(b -> b.getStatus().equals("ALTERACAO_SOLICITADA")).orElse(false);
        if (order.getClosedAt()!=null || !(order.getStage()==OrderStage.DIAGNOSTICO
                || (revision && order.getStage()==OrderStage.AGUARDANDO_APROVACAO)))
            throw new IllegalStateException("Não é possível publicar orçamento nesta etapa");
        if (!diagnoses.existsByOrderIdAndCompletedTrue(orderId))
            throw new IllegalStateException("Conclua o diagnóstico antes de publicar");
        if (order.getClient().getUserId()==null)
            throw new IllegalStateException("Vincule uma conta ao cliente antes de publicar");
        User linkedUser = users.findById(order.getClient().getUserId()).orElseThrow(() -> new IllegalStateException("Conta vinculada não encontrada"));
        if (!linkedUser.isActive() || linkedUser.getRole()!=Role.CLIENTE)
            throw new IllegalStateException("A conta vinculada deve ser um cliente ativo");
        if (last.isPresent() && !revision)
            throw new IllegalStateException("Já existe uma versão vigente para esta ordem");
        var items=planned.findByOrderIdOrderByCreatedAtAsc(orderId);
        if (items.isEmpty()) throw new IllegalArgumentException("Adicione peças ou serviços ao orçamento");
        BigDecimal total=items.stream().map(i -> i.getQuantity().multiply(i.getUnitPrice()))
                .reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        if (total.signum()<=0 || total.compareTo(new BigDecimal("999999999999.99"))>0)
            throw new IllegalArgumentException("O total do orçamento deve ser positivo e válido");
        Budget budget=new Budget(); budget.setOrder(order); budget.setVersion(last.map(b -> b.getVersion()+1).orElse(1));
        budget.setStatus("PENDENTE"); budget.setTotal(total); budget.setPublishedBy(actorId);
        budget=budgets.saveAndFlush(budget);
        for (PlannedItem item:items) {
            BudgetLine line=new BudgetLine(); line.setBudget(budget); line.setItemType(item.getItemType());
            line.setStockItem(item.getStockItem()); line.setDescription(item.getDescription());
            line.setQuantity(item.getQuantity()); line.setUnitPrice(item.getUnitPrice()); lines.save(line);
        }
        lines.flush();
        order.setValue(total.doubleValue()); order.setApproved(false); order.setReserved(false);
        order.setStage(OrderStage.AGUARDANDO_APROVACAO); order.setProgress(38); orders.save(order);
        audit.log(actorId,"ORCAMENTO_PUBLICADO",orderId.toString(),"Versão "+budget.getVersion());
        return view(budget);
    }
    @Transactional
    public BudgetView decide(UUID orderId, UUID budgetId, UUID actorId, String decision) {
        ServiceOrder order=lockedOrder(orderId);
        requireOwner(orderId, actorId);
        Budget budget=budgets.findFirstByOrderIdOrderByVersionDesc(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado"));
        if (!budget.getId().equals(budgetId)) throw new IllegalStateException("O orçamento não é a versão vigente");
        if (!Set.of("APROVAR","RECUSAR","SOLICITAR_ALTERACAO").contains(decision))
            throw new IllegalArgumentException("Decisão inválida");
        String target=switch(decision) {
            case "APROVAR" -> "APROVADO";
            case "RECUSAR" -> "RECUSADO";
            default -> "ALTERACAO_SOLICITADA";
        };
        if (budget.getStatus().equals(target) && actorId.equals(budget.getDecidedBy())) return view(budget);
        if (!budget.getStatus().equals("PENDENTE") || order.getClosedAt()!=null
                || order.getStage()!=OrderStage.AGUARDANDO_APROVACAO)
            throw new IllegalStateException("A decisão já foi registrada ou a ordem não está aguardando aprovação");
        budget.setStatus(target); budget.setDecidedBy(actorId); budget.setDecidedAt(Instant.now());
        if (decision.equals("APROVAR")) {
            order.setApproved(true);
            boolean complete=reserveAvailable(budget);
            order.setReserved(complete);
            order.setStage(complete ? OrderStage.AGUARDANDO_INICIO : OrderStage.AGUARDANDO_PECAS);
        } else if (decision.equals("RECUSAR")) {
            order.setApproved(false); order.setReserved(false);
            order.setClosedAt(Instant.now()); order.setClosedReason("RECUSA");
        } else {
            order.setApproved(false); order.setReserved(false);
        }
        orders.save(order); budgets.save(budget);
        audit.log(actorId,"ORCAMENTO_"+target,orderId.toString(),"Versão "+budget.getVersion());
        return view(budget);
    }
    @Transactional
    public BudgetView retryReservation(UUID orderId, UUID actorId) {
        ServiceOrder order=lockedOrder(orderId);
        if (order.getClosedAt()!=null || order.getStage()!=OrderStage.AGUARDANDO_PECAS)
            throw new IllegalStateException("A ordem não está aguardando peças");
        Budget budget=budgets.findFirstByOrderIdOrderByVersionDesc(orderId)
                .filter(b -> b.getStatus().equals("APROVADO"))
                .orElseThrow(() -> new IllegalStateException("Não há orçamento aprovado"));
        boolean complete=reserveAvailable(budget);
        order.setReserved(complete);
        if (complete) order.setStage(OrderStage.AGUARDANDO_INICIO);
        orders.save(order);
        audit.log(actorId,"RESERVAS_REAVALIADAS",orderId.toString(),complete ? "Completas" : "Pendentes");
        return view(budget);
    }
    private boolean reserveAvailable(Budget budget) {
        var pieces=lines.findByBudgetId(budget.getId()).stream().filter(x -> x.getStockItem()!=null)
                .sorted(Comparator.comparing(x -> x.getStockItem().getId())).toList();
        boolean complete=true;
        for (BudgetLine line:pieces) {
            StockItem item=stock.findByIdForUpdate(line.getStockItem().getId()).orElseThrow();
            PartReservation reservation=reservations.findByBudgetIdAndStockItemId(budget.getId(),item.getId()).orElse(null);
            BigDecimal already=reservation==null ? BigDecimal.ZERO : reservation.getQuantity();
            BigDecimal needed=line.getQuantity().subtract(already);
            BigDecimal available=BigDecimal.valueOf(item.getQuantity()).subtract(BigDecimal.valueOf(item.getReservedQuantity()));
            BigDecimal additional=needed.min(available.max(BigDecimal.ZERO));
            if (additional.signum()>0) {
                if (reservation==null) { reservation=new PartReservation(); reservation.setBudget(budget);
                    reservation.setStockItem(item); reservation.setQuantity(BigDecimal.ZERO); }
                reservation.setQuantity(already.add(additional));
                item.setReservedQuantity(BigDecimal.valueOf(item.getReservedQuantity()).add(additional).doubleValue());
                stock.save(item); reservations.save(reservation);
            }
            if (already.add(additional).compareTo(line.getQuantity())<0) complete=false;
        }
        return complete;
    }
    private ServiceOrder lockedOrder(UUID id) {
        return orders.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
    }
    private void requireOwner(UUID orderId, UUID actorId) {
        Client client=clients.findByUserId(actorId).orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        ServiceOrder order=orders.findById(orderId).orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (!order.getClient().getId().equals(client.getId())) throw new EntityNotFoundException("OS não encontrada");
    }
}
