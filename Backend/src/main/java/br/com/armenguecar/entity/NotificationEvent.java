package br.com.armenguecar.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="notification_outbox") @Getter @Setter
public class NotificationEvent {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(nullable=false) private UUID recipientId;
 @Column(nullable=false) private UUID orderId;
 @Column(nullable=false,length=200) private String title;
 @Column(nullable=false,length=1000) private String message;
 @Column(nullable=false,length=30) private String targetView;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 private Instant processedAt;
}
