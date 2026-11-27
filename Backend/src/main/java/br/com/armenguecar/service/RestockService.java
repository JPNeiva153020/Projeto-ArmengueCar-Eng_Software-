package br.com.armenguecar.service;
import br.com.armenguecar.entity.*;
import br.com.armenguecar.enums.MovementType;
import br.com.armenguecar.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Service
public class RestockService {
 private final RestockRequestRepository requests; private final StockItemRepository stock; private final StockMovementRepository movements; private final UserRepository users; private final AuditService audit;
 public RestockService(RestockRequestRepository requests,StockItemRepository stock,StockMovementRepository movements,UserRepository users,AuditService audit){this.requests=requests;this.stock=stock;this.movements=movements;this.users=users;this.audit=audit;}
 @Transactional(readOnly=true) public List<RestockRequest> list(){return requests.findAllByOrderByCreatedAtDesc();}
 @Transactional public RestockRequest create(UUID itemId,BigDecimal quantity,String note,UUID actor){
  StockItem item=stock.findById(itemId).orElseThrow(()->new EntityNotFoundException("Peça não encontrada"));
  RestockRequest r=new RestockRequest();r.setItem(item);r.setQuantity(quantity);r.setNote(note==null || note.isBlank()?null:note.trim());r.setCreatedBy(actor);
  r=requests.saveAndFlush(r);audit.log(actor,"REPOSICAO_SOLICITADA",r.getId().toString(),"Item: "+item.getCode()+"; quantidade: "+quantity);return r;
 }
 @Transactional public RestockRequest resolve(UUID id,String action,UUID actor){
  RestockRequest r=requests.findForUpdate(id).orElseThrow(()->new EntityNotFoundException("Solicitação não encontrada"));
  String target=switch(action){case "receive" -> "RECEBIDA";case "cancel" -> "CANCELADA";default -> throw new IllegalArgumentException("Ação inválida");};
  if(r.getStatus().equals(target)) return r;
  if(!r.getStatus().equals("ABERTA")) throw new IllegalStateException("Solicitação já encerrada");
  if(target.equals("RECEBIDA")){
   StockItem item=stock.findByIdForUpdate(r.getItem().getId()).orElseThrow();
   BigDecimal total=BigDecimal.valueOf(item.getQuantity()).add(r.getQuantity());
   if(total.compareTo(new BigDecimal("9999999999.99"))>0) throw new IllegalArgumentException("Saldo excede o limite permitido");
   item.setQuantity(total.doubleValue());stock.save(item);
   StockMovement m=new StockMovement();m.setItem(item);m.setType(MovementType.ENTRADA);m.setQuantity(r.getQuantity().doubleValue());m.setReason("Recebimento da solicitação "+id);m.setCreatedBy(users.findById(actor).orElseThrow());movements.save(m);
  }
  r.setStatus(target);r.setResolvedBy(actor);r.setResolvedAt(Instant.now());requests.save(r);audit.log(actor,"REPOSICAO_"+target,id.toString(),"Item: "+r.getItem().getCode());return r;
 }
}
