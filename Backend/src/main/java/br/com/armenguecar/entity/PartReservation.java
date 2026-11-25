package br.com.armenguecar.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="part_reservations") @Getter @Setter
public class PartReservation {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="budget_id") @JsonIgnore private Budget budget;
    @ManyToOne(optional=false) @JoinColumn(name="stock_item_id") private StockItem stockItem;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal quantity;
    @Column(precision=12, scale=2) private BigDecimal usedQuantity;
    @Column(precision=12, scale=2) private BigDecimal releasedQuantity;
}
