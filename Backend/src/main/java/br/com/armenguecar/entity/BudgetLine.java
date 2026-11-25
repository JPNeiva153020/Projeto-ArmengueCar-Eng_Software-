package br.com.armenguecar.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="budget_lines") @Getter @Setter
public class BudgetLine {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="budget_id") @JsonIgnore private Budget budget;
    @Column(nullable=false, length=20) private String itemType;
    @ManyToOne @JoinColumn(name="stock_item_id") private StockItem stockItem;
    @Column(nullable=false) private String description;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal quantity;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal unitPrice;
}
