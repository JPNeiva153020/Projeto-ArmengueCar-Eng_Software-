package br.com.armenguecar.repository;
import br.com.armenguecar.entity.RestockRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface RestockRequestRepository extends JpaRepository<RestockRequest,UUID> {
 List<RestockRequest> findAllByOrderByCreatedAtDesc();
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RestockRequest r where r.id=:id") Optional<RestockRequest> findForUpdate(@Param("id") UUID id);
}
