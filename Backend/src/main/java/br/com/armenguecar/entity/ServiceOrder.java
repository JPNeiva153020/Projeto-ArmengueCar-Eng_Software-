package br.com.armenguecar.entity;

import br.com.armenguecar.enums.OrderStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_orders")
@Getter
@Setter
@NoArgsConstructor
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String number;

    @ManyToOne(optional = false)
    private Client client;

    @Column(nullable = false)
    private String plate;

    @Column(nullable = false)
    private String vehicle;

    @Column(nullable = false, length = 2000)
    private String problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStage stage = OrderStage.RECEPCAO_CHECKIN;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false)
    private boolean approved;

    @Column(nullable = false)
    private boolean reserved;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false)
    private int days;

    @Column(nullable = false)
    private int photos;

    @Column(nullable = false)
    private String ownerInitials;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(nullable = false, length = 1000)
    private String checklist = "[false, false, false, false, false]";

    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
