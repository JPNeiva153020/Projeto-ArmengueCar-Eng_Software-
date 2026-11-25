package br.com.armenguecar.repository;
import br.com.armenguecar.entity.PartReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface PartReservationRepository extends JpaRepository<PartReservation, UUID> {
    List<PartReservation> findByBudgetId(UUID budgetId);
    Optional<PartReservation> findByBudgetIdAndStockItemId(UUID budgetId, UUID stockItemId);
}
