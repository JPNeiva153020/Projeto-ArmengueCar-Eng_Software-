package br.com.armenguecar.repository;

import br.com.armenguecar.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<StockItem> findByNameIgnoreCase(String name);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from StockItem s where s.id = :id")
    Optional<StockItem> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
