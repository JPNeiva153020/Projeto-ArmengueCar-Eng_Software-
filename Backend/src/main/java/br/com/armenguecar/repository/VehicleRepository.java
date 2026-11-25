package br.com.armenguecar.repository;

import br.com.armenguecar.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByPlateIgnoreCase(String plate);
}
