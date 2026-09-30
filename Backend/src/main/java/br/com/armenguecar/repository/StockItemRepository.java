package br.com.armenguecar.repository;

import br.com.armenguecar.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    Optional<StockItem> findByNameIgnoreCase(String name);
}
