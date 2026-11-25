package br.com.armenguecar.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_diagnoses")
@Getter @Setter
public class Diagnosis {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @JsonIgnore @OneToOne(optional = false)
    @JoinColumn(name = "order_id", unique = true, nullable = false)
    private ServiceOrder order;
    @Column(nullable = false, length = 2000) private String damages = "";
    @Column(nullable = false, length = 2000) private String symptoms = "";
    @Column(nullable = false, length = 2000) private String observations = "";
    @Column(nullable = false) private boolean completed;
    private UUID updatedBy;
    @Column(nullable = false) private Instant updatedAt = Instant.now();
}
