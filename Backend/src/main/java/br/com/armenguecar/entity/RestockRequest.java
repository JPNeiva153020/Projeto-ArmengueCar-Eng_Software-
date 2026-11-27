package br.com.armenguecar.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="restock_requests") @Getter @Setter
public class RestockRequest {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(optional=false) @JoinColumn(name="item_id") private StockItem item;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal quantity;
 @Column(nullable=false,length=20) private String status="ABERTA";
 @Column(length=1000) private String note;
 @Column(nullable=false) private UUID createdBy;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 private UUID resolvedBy;
 private Instant resolvedAt;
}
