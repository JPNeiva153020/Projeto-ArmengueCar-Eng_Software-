package br.com.armenguecar.repository;

import br.com.armenguecar.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, UUID> {
    java.util.List<ServiceOrder> findByClientId(UUID clientId);
    boolean existsByVehicleRefId(UUID vehicleId);
}
