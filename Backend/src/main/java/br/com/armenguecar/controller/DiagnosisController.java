package br.com.armenguecar.controller;

import br.com.armenguecar.entity.Diagnosis;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.repository.DiagnosisRepository;
import br.com.armenguecar.repository.ServiceOrderRepository;
import br.com.armenguecar.service.AuditService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/diagnosis")
public class DiagnosisController {
    private final DiagnosisRepository diagnoses;
    private final ServiceOrderRepository orders;
    private final AuditService audit;
    public DiagnosisController(DiagnosisRepository diagnoses, ServiceOrderRepository orders, AuditService audit) {
        this.diagnoses = diagnoses; this.orders = orders; this.audit = audit;
    }
    public record DiagnosisRequest(@Size(max=2000) String damages, @Size(max=2000) String symptoms,
                                   @Size(max=2000) String observations, boolean completed) {}
    @GetMapping @Transactional(readOnly = true)
    public ResponseEntity<Diagnosis> get(@PathVariable UUID orderId) {
        if (!orders.existsById(orderId)) throw new EntityNotFoundException("OS não encontrada");
        return diagnoses.findByOrderId(orderId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
    @PutMapping @Transactional
    public Diagnosis save(@PathVariable UUID orderId, @Valid @RequestBody DiagnosisRequest request, Authentication auth) {
        var order = orders.findById(orderId).orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt() != null || order.getStage() != OrderStage.DIAGNOSTICO)
            throw new IllegalStateException("O diagnóstico só pode ser registrado na etapa de diagnóstico");
        String damages = clean(request.damages()), symptoms = clean(request.symptoms());
        if (request.completed() && (damages.isBlank() || symptoms.isBlank()))
            throw new IllegalArgumentException("Informe avarias e sintomas para concluir o diagnóstico");
        var diagnosis = diagnoses.findByOrderId(orderId).orElseGet(Diagnosis::new);
        diagnosis.setOrder(order); diagnosis.setDamages(damages); diagnosis.setSymptoms(symptoms);
        diagnosis.setObservations(clean(request.observations())); diagnosis.setCompleted(request.completed());
        var user = (User) auth.getPrincipal();
        diagnosis.setUpdatedBy(user.getId()); diagnosis.setUpdatedAt(Instant.now());
        Diagnosis saved = diagnoses.save(diagnosis);
        audit.log(user.getId(), request.completed() ? "DIAGNOSTICO_CONCLUIDO" : "DIAGNOSTICO_ATUALIZADO",
                orderId.toString(), null);
        return saved;
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
}
