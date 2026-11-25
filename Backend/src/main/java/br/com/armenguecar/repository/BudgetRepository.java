package br.com.armenguecar.repository;
import br.com.armenguecar.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    Optional<Budget> findFirstByOrderIdOrderByVersionDesc(UUID orderId);
    List<Budget> findByOrderIdOrderByVersionDesc(UUID orderId);
}
