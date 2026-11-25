package br.com.armenguecar.repository;
import br.com.armenguecar.entity.BudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface BudgetLineRepository extends JpaRepository<BudgetLine, UUID> {
    List<BudgetLine> findByBudgetId(UUID budgetId);
}
