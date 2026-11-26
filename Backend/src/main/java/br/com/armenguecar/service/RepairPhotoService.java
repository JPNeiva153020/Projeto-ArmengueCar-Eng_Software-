package br.com.armenguecar.service;
import br.com.armenguecar.entity.*;
import br.com.armenguecar.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.util.*;
@Service
public class RepairPhotoService {
 private final ServiceOrderRepository orders; private final ClientRepository clients; private final RepairPhotoRepository photos; private final AuditService audit;
 public RepairPhotoService(ServiceOrderRepository orders,ClientRepository clients,RepairPhotoRepository photos,AuditService audit) {this.orders=orders;this.clients=clients;this.photos=photos;this.audit=audit;}
 private ServiceOrder owner(UUID orderId,UUID actor) {
  ServiceOrder o=orders.findById(orderId).orElseThrow(()->new EntityNotFoundException("OS não encontrada"));
  if(clients.findByUserId(actor).filter(c->c.getId().equals(o.getClient().getId())).isEmpty()) throw new EntityNotFoundException("OS não encontrada");
  return o;
 }
 @Transactional(readOnly=true) public List<RepairPhotoRepository.PhotoSummary> list(UUID orderId,UUID actor,boolean client) {
  if(client) owner(orderId,actor); else if(!orders.existsById(orderId)) throw new EntityNotFoundException("OS não encontrada");
  return photos.summaries(orderId,client);
 }
 @Transactional(readOnly=true) public RepairPhoto get(UUID orderId,UUID id,UUID actor,boolean client) {
  if(client) owner(orderId,actor);
  RepairPhoto p=photos.findByIdAndOrderId(id,orderId).orElseThrow(()->new EntityNotFoundException("Foto não encontrada"));
  if(client && !p.isVisibleToClient()) throw new EntityNotFoundException("Foto não encontrada");
  return p;
 }
 @Transactional public UUID upload(UUID orderId,UUID actor,MultipartFile file,boolean visible) throws IOException {
  ServiceOrder o=orders.findByIdForUpdate(orderId).orElseThrow(()->new EntityNotFoundException("OS não encontrada"));
  if(o.getClosedAt()!=null) throw new IllegalStateException("OS encerrada");
  if(file.isEmpty() || file.getSize()>5_000_000) throw new IllegalArgumentException("Selecione uma imagem de até 5 MB");
  byte[] data=file.getBytes(); String type;
  try(ImageInputStream input=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
   if(input==null) throw new IllegalArgumentException("Imagem inválida");
   Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
   if(!readers.hasNext()) throw new IllegalArgumentException("Envie uma imagem JPEG ou PNG válida");
   ImageReader reader=readers.next();
   try {reader.setInput(input,true,true); String format=reader.getFormatName().toLowerCase(Locale.ROOT);
    if(!format.equals("jpeg") && !format.equals("png")) throw new IllegalArgumentException("Envie uma imagem JPEG ou PNG");
    int w=reader.getWidth(0),h=reader.getHeight(0);
    if(w<=0 || h<=0 || (long)w*h>24_000_000) throw new IllegalArgumentException("Imagem excede o limite de resolução");
    if(reader.read(0)==null) throw new IllegalArgumentException("Imagem inválida");
    type=format.equals("png") ? "image/png" : "image/jpeg";
   } finally {reader.dispose();}
  } catch (IOException e) {throw new IllegalArgumentException("Imagem inválida",e);}
  RepairPhoto photo=new RepairPhoto();photo.setOrderId(orderId);photo.setStage(o.getStage().name());photo.setContentType(type);photo.setContent(data);photo.setVisibleToClient(visible);photo.setUploadedBy(actor);
  photo=photos.saveAndFlush(photo); o.setPhotos(o.getPhotos()+1);orders.save(o);audit.log(actor,"FOTO_ANEXADA",orderId.toString(),"Etapa: "+o.getStage()+"; visível ao cliente: "+visible);return photo.getId();
 }
}
