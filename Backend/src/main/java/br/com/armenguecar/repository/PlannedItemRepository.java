package br.com.armenguecar.repository;
import br.com.armenguecar.entity.PlannedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface PlannedItemRepository extends JpaRepository<PlannedItem, UUID> {
    List<PlannedItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
    boolean existsByOrderIdAndStockItemId(UUID orderId, UUID stockItemId);
}
