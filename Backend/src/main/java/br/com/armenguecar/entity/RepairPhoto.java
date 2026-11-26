package br.com.armenguecar.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="repair_photos") @Getter @Setter
public class RepairPhoto {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(nullable=false) private UUID orderId;
 @Column(nullable=false,length=40) private String stage;
 @Column(nullable=false,length=20) private String contentType;
 @Column(nullable=false,columnDefinition="bytea") private byte[] content;
 @Column(nullable=false) private boolean visibleToClient;
 @Column(nullable=false) private UUID uploadedBy;
 @Column(nullable=false) private Instant createdAt=Instant.now();
}
