package br.com.armenguecar.controller;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.service.RepairPhotoService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
@RestController
public class RepairPhotoController {
 private final RepairPhotoService service;
 public RepairPhotoController(RepairPhotoService service){this.service=service;}
 private UUID actor(Authentication a){return ((User)a.getPrincipal()).getId();}
 @GetMapping("/api/orders/{id}/photos") public Object list(@PathVariable UUID id,Authentication a){return service.list(id,actor(a),false);}
 @PostMapping(value="/api/orders/{id}/photos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Map<String,UUID> upload(@PathVariable UUID id,@RequestPart("file") MultipartFile file,@RequestParam(defaultValue="false") boolean visibleToClient,Authentication a) throws IOException {return Map.of("id",service.upload(id,actor(a),file,visibleToClient));}
 @GetMapping("/api/orders/{id}/photos/{photoId}") public ResponseEntity<byte[]> get(@PathVariable UUID id,@PathVariable UUID photoId,Authentication a){return response(service.get(id,photoId,actor(a),false));}
 @GetMapping("/api/portal/orders/{id}/photos") @org.springframework.security.access.prepost.PreAuthorize("hasRole('CLIENTE')") public Object clientList(@PathVariable UUID id,Authentication a){return service.list(id,actor(a),true);}
 @GetMapping("/api/portal/orders/{id}/photos/{photoId}") @org.springframework.security.access.prepost.PreAuthorize("hasRole('CLIENTE')") public ResponseEntity<byte[]> clientGet(@PathVariable UUID id,@PathVariable UUID photoId,Authentication a){return response(service.get(id,photoId,actor(a),true));}
 private ResponseEntity<byte[]> response(br.com.armenguecar.entity.RepairPhoto photo){return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.getContentType())).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(photo.getContent());}
}
