package br.com.armenguecar.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planned_items")
@Getter @Setter
public class PlannedItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id", nullable = false) @JsonIgnore
    private ServiceOrder order;
    @Column(name = "item_type", nullable = false, length = 20) private String itemType;
    @ManyToOne @JoinColumn(name = "stock_item_id") private StockItem stockItem;
    @Column(nullable = false) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantity;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false) private UUID createdBy;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
