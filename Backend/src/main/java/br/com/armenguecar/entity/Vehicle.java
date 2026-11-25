package br.com.armenguecar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter @Setter
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    @Column(nullable = false, unique = true)
    private String plate;
    @Column(nullable = false)
    private String model;
    private String brand;
    @Column(name = "model_year")
    private Integer modelYear;
}
