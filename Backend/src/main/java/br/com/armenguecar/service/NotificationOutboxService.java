package br.com.armenguecar.service;
import br.com.armenguecar.entity.*;
import br.com.armenguecar.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;
@Service
public class NotificationOutboxService {
 private final NotificationEventRepository events; private final NotificationRepository notifications; private final UserRepository users;
 public NotificationOutboxService(NotificationEventRepository events,NotificationRepository notifications,UserRepository users){this.events=events;this.notifications=notifications;this.users=users;}
 @Transactional(propagation=Propagation.MANDATORY)
 public void enqueue(UUID recipient, UUID orderId,String title,String message,String target) {
  if(recipient==null) return;
  NotificationEvent e=new NotificationEvent();e.setRecipientId(recipient);e.setOrderId(orderId);e.setTitle(title);e.setMessage(message);e.setTargetView(target);events.save(e);
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void process(UUID eventId, UUID recipient) {
  NotificationEvent e=events.findForUpdate(eventId).orElseThrow();
  if(e.getProcessedAt()!=null || !e.getRecipientId().equals(recipient)) return;
  User user=users.findById(recipient).orElseThrow();
  Notification n=new Notification();n.setUser(user);n.setTitle(e.getTitle());n.setMessage(e.getMessage());n.setLevel("info");n.setTargetView(e.getTargetView());n.setTargetOrderId(e.getOrderId());
  notifications.saveAndFlush(n);e.setProcessedAt(Instant.now());events.save(e);
 }
}
