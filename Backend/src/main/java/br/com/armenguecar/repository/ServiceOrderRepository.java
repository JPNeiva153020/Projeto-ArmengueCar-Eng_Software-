package br.com.armenguecar.repository;

import br.com.armenguecar.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, UUID> {
    java.util.List<ServiceOrder> findByClientId(UUID clientId);
    boolean existsByVehicleRefId(UUID vehicleId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from ServiceOrder o where o.id = :id")
    java.util.Optional<ServiceOrder> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
