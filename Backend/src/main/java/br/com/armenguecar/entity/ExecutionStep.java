package br.com.armenguecar.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="execution_steps") @Getter @Setter
public class ExecutionStep {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(nullable=false) private UUID orderId;
 @Column(nullable=false,length=40) private String kind;
 @Column(nullable=false,length=20) private String status;
 @Column(nullable=false) private Instant startedAt;
 private Instant activeSince;
 private Instant endedAt;
 @Column(nullable=false) private long activeMillis;
 @Column(nullable=false) private UUID startedBy;
 private UUID finishedBy;
}
