package br.com.armenguecar.repository;
import br.com.armenguecar.entity.NotificationEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface NotificationEventRepository extends JpaRepository<NotificationEvent,UUID> {
 List<NotificationEvent> findTop100ByRecipientIdAndProcessedAtIsNullOrderByCreatedAtAsc(UUID recipientId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from NotificationEvent e where e.id=:id") Optional<NotificationEvent> findForUpdate(@Param("id") UUID id);
}
