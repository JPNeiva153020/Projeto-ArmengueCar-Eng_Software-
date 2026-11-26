package br.com.armenguecar.repository;
import br.com.armenguecar.entity.ExecutionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ExecutionStepRepository extends JpaRepository<ExecutionStep, UUID> {
 List<ExecutionStep> findByOrderIdOrderByStartedAtAsc(UUID orderId);
}
