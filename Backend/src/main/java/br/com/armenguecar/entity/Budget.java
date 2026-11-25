package br.com.armenguecar.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="budgets") @Getter @Setter
public class Budget {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="order_id") @JsonIgnore private ServiceOrder order;
    @Column(nullable=false) private int version;
    @Column(nullable=false, length=30) private String status;
    @Column(nullable=false, precision=14, scale=2) private BigDecimal total;
    @Column(nullable=false) private UUID publishedBy;
    @Column(nullable=false) private Instant publishedAt=Instant.now();
    private UUID decidedBy;
    private Instant decidedAt;
}
