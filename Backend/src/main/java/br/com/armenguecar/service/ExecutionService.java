package br.com.armenguecar.service;
import br.com.armenguecar.entity.*;
import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class ExecutionService {
 private static final Set<String> KINDS=Set.of("FUNILARIA","PINTURA","ESTRUTURA","MONTAGEM","OUTRO");
 private final BudgetRepository budgets; private final ServiceOrderRepository orders; private final ExecutionStepRepository steps; private final AuditService audit;
 public ExecutionService(ServiceOrderRepository orders, ExecutionStepRepository steps, AuditService audit, BudgetRepository budgets) { this.orders=orders;this.steps=steps;this.audit=audit;this.budgets=budgets; }
 public record StepView(UUID id,String kind,String status,Instant startedAt,Instant endedAt,long activeMillis,UUID startedBy,UUID finishedBy) {}
 private StepView view(ExecutionStep s) {
  long elapsed=s.getActiveMillis(); if (s.getActiveSince()!=null) elapsed+=Math.max(0,Duration.between(s.getActiveSince(),Instant.now()).toMillis());
  return new StepView(s.getId(),s.getKind(),s.getStatus(),s.getStartedAt(),s.getEndedAt(),elapsed,s.getStartedBy(),s.getFinishedBy());
 }
 @Transactional(readOnly=true) public List<StepView> list(UUID orderId) {
  if (!orders.existsById(orderId)) throw new EntityNotFoundException("OS não encontrada");
  return steps.findByOrderIdOrderByStartedAtAsc(orderId).stream().map(this::view).toList();
 }
 private ServiceOrder lock(UUID id) {
  ServiceOrder o=orders.findByIdForUpdate(id).orElseThrow(()->new EntityNotFoundException("OS não encontrada"));
  if(o.getClosedAt()!=null || o.getStage()!=OrderStage.EXECUCAO || !o.isApproved() || !o.isReserved() || budgets.findFirstByOrderIdOrderByVersionDesc(id).filter(b -> b.getStatus().equals("APROVADO")).isEmpty()) throw new IllegalStateException("OS não está em execução com orçamento e reservas aprovados");
  return o;
 }
 @Transactional public StepView start(UUID orderId,String kind,UUID actor) {
  lock(orderId); if (!KINDS.contains(kind)) throw new IllegalArgumentException("Tipo de etapa inválido");
  if(steps.findByOrderIdOrderByStartedAtAsc(orderId).stream().anyMatch(s->!s.getStatus().equals("CONCLUIDA"))) throw new IllegalStateException("Conclua a etapa anterior antes de iniciar outra");
  ExecutionStep s=new ExecutionStep(); s.setOrderId(orderId);s.setKind(kind);s.setStatus("EM_ANDAMENTO");s.setStartedAt(Instant.now());s.setActiveSince(s.getStartedAt());s.setStartedBy(actor);
  s=steps.saveAndFlush(s);audit.log(actor,"ETAPA_INICIADA",orderId.toString(),kind);return view(s);
 }
 @Transactional public StepView transition(UUID orderId,UUID stepId,String action,UUID actor) {
  lock(orderId); ExecutionStep s=steps.findById(stepId).filter(x->x.getOrderId().equals(orderId)).orElseThrow(()->new EntityNotFoundException("Etapa não encontrada"));
  Instant now=Instant.now();
  switch(action) {
   case "pause" -> { if(!s.getStatus().equals("EM_ANDAMENTO")) throw new IllegalStateException("Etapa não está em andamento"); accumulate(s,now);s.setStatus("PAUSADA"); }
   case "resume" -> { if(!s.getStatus().equals("PAUSADA")) throw new IllegalStateException("Etapa não está pausada");s.setActiveSince(now);s.setStatus("EM_ANDAMENTO"); }
   case "finish" -> { if(!s.getStatus().equals("EM_ANDAMENTO") && !s.getStatus().equals("PAUSADA")) throw new IllegalStateException("Etapa já concluída"); if(s.getActiveSince()!=null) accumulate(s,now);s.setStatus("CONCLUIDA");s.setEndedAt(now);s.setFinishedBy(actor); }
   default -> throw new IllegalArgumentException("Ação inválida");
  }
  steps.save(s);audit.log(actor,"ETAPA_"+action.toUpperCase(Locale.ROOT),orderId.toString(),s.getKind()); return view(s);
 }
 private void accumulate(ExecutionStep s,Instant now) { s.setActiveMillis(s.getActiveMillis()+Math.max(0,Duration.between(s.getActiveSince(),now).toMillis()));s.setActiveSince(null); }
}
