package br.com.armenguecar.repository;
import br.com.armenguecar.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
    Optional<Diagnosis> findByOrderId(UUID orderId);
    boolean existsByOrderIdAndCompletedTrue(UUID orderId);
}
