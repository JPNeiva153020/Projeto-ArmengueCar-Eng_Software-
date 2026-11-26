package br.com.armenguecar.repository;
import br.com.armenguecar.entity.RepairPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface RepairPhotoRepository extends JpaRepository<RepairPhoto,UUID> {
 interface PhotoSummary { UUID getId(); String getStage(); boolean getVisibleToClient(); Instant getCreatedAt(); UUID getUploadedBy(); }
 @Query("select p.id as id,p.stage as stage,p.visibleToClient as visibleToClient,p.createdAt as createdAt,p.uploadedBy as uploadedBy from RepairPhoto p where p.orderId=:orderId and (:clientOnly=false or p.visibleToClient=true) order by p.createdAt desc")
 List<PhotoSummary> summaries(@Param("orderId") UUID orderId,@Param("clientOnly") boolean clientOnly);
 Optional<RepairPhoto> findByIdAndOrderId(UUID id,UUID orderId);
}
