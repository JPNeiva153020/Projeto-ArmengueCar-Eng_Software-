package br.com.armenguecar.service;

import br.com.armenguecar.dto.OrderDtos.ChecklistRequest;
import br.com.armenguecar.dto.OrderDtos.CreateOrderRequest;
import br.com.armenguecar.dto.OrderDtos.DecisionRequest;
import br.com.armenguecar.dto.OrderDtos.UpdateBudgetRequest;
import br.com.armenguecar.entity.Client;
import br.com.armenguecar.entity.ServiceOrder;
import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.repository.ClientRepository;
import br.com.armenguecar.repository.ServiceOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final int TOTAL_STAGES = OrderStage.values().length;
    private static final int INITIAL_PROGRESS = 5;
    private static final String DEFAULT_OWNER = "MA";
    private static final String COMPLETE_CHECKLIST = "[true, true, true, true, true]";

    private final ServiceOrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final br.com.armenguecar.repository.VehicleRepository vehicles;
    private final br.com.armenguecar.repository.DiagnosisRepository diagnoses;
    private final br.com.armenguecar.repository.BudgetRepository budgets;
    private final AuditService audit;

    public OrderService(
            ServiceOrderRepository orderRepository,
            ClientRepository clientRepository,
            br.com.armenguecar.repository.VehicleRepository vehicles,
            br.com.armenguecar.repository.DiagnosisRepository diagnoses,
            br.com.armenguecar.repository.BudgetRepository budgets, AuditService audit
    ) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.vehicles = vehicles;
        this.diagnoses = diagnoses;
        this.budgets = budgets;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ServiceOrder> list() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ServiceOrder get(UUID id) {
        return findOrder(id);
    }

    @Transactional
    public ServiceOrder create(CreateOrderRequest request) {
        var vehicle = vehicles.findByIdForUpdate(request.vehicleId())
                .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado"));
        Client client = vehicle.getClient();
        String plate = normalizePlate(vehicle.getPlate());
        validateNoActiveOrder(vehicle, plate);

        ServiceOrder order = new ServiceOrder();
        order.setNumber(generateNextNumber());
        order.setClient(client);
        order.setVehicleRef(vehicle);
        order.setPlate(plate);
        order.setVehicle(vehicle.getModel());
        order.setProblem(request.problem().trim());
        order.setOwnerInitials(DEFAULT_OWNER);
        order.setProgress(INITIAL_PROGRESS);

        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder advance(UUID id) {
        ServiceOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt() != null) throw new IllegalStateException("OS já encerrada");
        switch (order.getStage()) {
            case RECEPCAO_CHECKIN -> order.setStage(OrderStage.DIAGNOSTICO);
            case AGUARDANDO_INICIO -> {
                if (!order.isApproved() || !order.isReserved() || !hasApprovedBudget(id))
                    throw new IllegalStateException("Aprovação e reservas completas são necessárias");
                order.setStage(OrderStage.EXECUCAO);
            }
            case EXECUCAO -> {
                if (!hasApprovedBudget(id)) throw new IllegalStateException("Orçamento aprovado não encontrado; revise esta ordem antiga");
                order.setStage(OrderStage.CONTROLE_QUALIDADE);
            }
            case DIAGNOSTICO -> throw new IllegalStateException("Publique o orçamento por itens para solicitar aprovação");
            case AGUARDANDO_APROVACAO -> throw new IllegalStateException("Aguardando decisão do cliente");
            case AGUARDANDO_PECAS -> throw new IllegalStateException("Reavalie as reservas após a reposição");
            case CONTROLE_QUALIDADE -> throw new IllegalStateException("Conclua o checklist para liberar a entrega");
            case PRONTO_ENTREGA -> throw new IllegalStateException("OS já está pronta para entrega");
        }
        order.setProgress(progressFor(order.getStage()));
        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder retreat(UUID id) {
        throw new IllegalStateException("Retorno manual indisponível; use o fluxo de retrabalho");
    }

    @Transactional
    public ServiceOrder updateBudget(UUID id, UpdateBudgetRequest request) {
        throw new IllegalStateException("Use o orçamento versionado por itens");
    }

    @Transactional
    public ServiceOrder decide(UUID id, DecisionRequest request) {
        throw new IllegalStateException("A decisão deve ser registrada pelo cliente vinculado");
    }

    @Transactional
    public ServiceOrder rework(UUID id, String reason, UUID actorId) {
        ServiceOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt() != null || order.getStage() != OrderStage.CONTROLE_QUALIDADE)
            throw new IllegalStateException("Retrabalho disponível somente no controle de qualidade");
        if (!order.isApproved() || !order.isReserved() || !hasApprovedBudget(id))
            throw new IllegalStateException("Orçamento aprovado e reservas são necessários para retrabalho");
        order.setStage(OrderStage.EXECUCAO);
        order.setChecklist("[false, false, false, false, false]");
        order.setProgress(progressFor(OrderStage.EXECUCAO));
        audit.log(actorId, "RETRABALHO", id.toString(), reason.trim());
        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder updateChecklist(UUID id, ChecklistRequest request) {
        if (request.checklist().size() != 5) {
            throw new IllegalArgumentException("Checklist deve conter exatamente 5 itens");
        }

        ServiceOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
        if (order.getClosedAt() != null || order.getStage() != OrderStage.CONTROLE_QUALIDADE) {
            throw new IllegalStateException("Checklist disponível somente no controle de qualidade");
        }
        String serializedChecklist = request.checklist().toString();
        boolean completed = request.checklist().stream().allMatch(Boolean::booleanValue);

        order.setChecklist(serializedChecklist);

        if (completed) {
            order.setStage(OrderStage.PRONTO_ENTREGA);
            order.setProgress(100);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder deliver(UUID id) {
        throw new IllegalStateException("Entrega indisponível até a baixa de peças ser implementada");
    }

    @Transactional
    public ServiceOrder addPhoto(UUID id) {
        throw new IllegalStateException("Envio de fotos ainda não implementado");
    }

    private boolean hasApprovedBudget(UUID id) {
        return budgets.findFirstByOrderIdOrderByVersionDesc(id)
                .map(budget -> budget.getStatus().equals("APROVADO")).orElse(false);
    }

    private ServiceOrder findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
    }

    private void validateNoActiveOrder(br.com.armenguecar.entity.Vehicle vehicle, String plate) {
        boolean hasActiveOrder = orderRepository.findAll().stream()
                .anyMatch(order -> order.getClosedAt() == null
                        && (plate.equalsIgnoreCase(order.getPlate())
                        || (order.getVehicleRef() != null && vehicle.getId().equals(order.getVehicleRef().getId()))));

        if (hasActiveOrder) {
            throw new IllegalArgumentException("Veículo já possui OS ativa");
        }
    }

    private String generateNextNumber() {
        int highestNumber = orderRepository.findAll().stream()
                .map(ServiceOrder::getNumber)
                .mapToInt(this::extractNumber)
                .max()
                .orElse(1051);

        return "OS-" + (highestNumber + 1);
    }

    private int extractNumber(String number) {
        try {
            return Integer.parseInt(number.replace("OS-", ""));
        } catch (NumberFormatException exception) {
            return 1051;
        }
    }

    private int progressFor(OrderStage stage) {
        if (stage == OrderStage.PRONTO_ENTREGA) {
            return 100;
        }

        return Math.max(
                INITIAL_PROGRESS,
                Math.round((stage.ordinal() + 1) * 100f / TOTAL_STAGES)
        );
    }

    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase();
    }
}
