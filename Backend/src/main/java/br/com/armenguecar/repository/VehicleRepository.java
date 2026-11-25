package br.com.armenguecar.repository;

import br.com.armenguecar.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByPlateIgnoreCase(String plate);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Vehicle v where v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
