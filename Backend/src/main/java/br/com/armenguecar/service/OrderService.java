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

    public OrderService(
            ServiceOrderRepository orderRepository,
            ClientRepository clientRepository,
            br.com.armenguecar.repository.VehicleRepository vehicles,
            br.com.armenguecar.repository.DiagnosisRepository diagnoses
    ) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.vehicles = vehicles;
        this.diagnoses = diagnoses;
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
        ServiceOrder order = findOrder(id);
        OrderStage currentStage = order.getStage();

        if (currentStage == OrderStage.PRONTO_ENTREGA) {
            throw new IllegalStateException("OS já está pronta para entrega");
        }

        if (currentStage == OrderStage.DIAGNOSTICO && !diagnoses.existsByOrderIdAndCompletedTrue(id)) {
            throw new IllegalStateException("Conclua o diagnóstico antes de enviar o orçamento");
        }
        if (currentStage == OrderStage.DIAGNOSTICO && order.getValue() <= 0) {
            throw new IllegalStateException("Informe um orçamento maior que zero antes de enviar para aprovação");
        }
        if (currentStage == OrderStage.CONTROLE_QUALIDADE) {
            throw new IllegalStateException("Conclua o checklist para liberar a entrega");
        }
        if (currentStage == OrderStage.AGUARDANDO_APROVACAO && !order.isApproved()) {
            throw new IllegalStateException("Orçamento ainda não aprovado");
        }

        OrderStage nextStage = OrderStage.values()[currentStage.ordinal() + 1];
        order.setStage(nextStage);
        order.setProgress(progressFor(nextStage));

        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder retreat(UUID id) {
        ServiceOrder order = findOrder(id);
        OrderStage currentStage = order.getStage();

        if (currentStage == OrderStage.RECEPCAO_CHECKIN) {
            throw new IllegalStateException("OS já está na primeira etapa");
        }

        OrderStage previousStage = OrderStage.values()[currentStage.ordinal() - 1];
        order.setStage(previousStage);
        order.setProgress(progressFor(previousStage));

        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder updateBudget(UUID id, UpdateBudgetRequest request) {
        ServiceOrder order = findOrder(id);
        if (order.getClosedAt() != null || order.getStage() != OrderStage.DIAGNOSTICO
                || !diagnoses.existsByOrderIdAndCompletedTrue(id))
            throw new IllegalStateException("Conclua o diagnóstico antes de elaborar o orçamento");
        order.setValue(request.value());
        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder decide(UUID id, DecisionRequest request) {
        ServiceOrder order = findOrder(id);

        if (order.getStage() != OrderStage.AGUARDANDO_APROVACAO) {
            throw new IllegalStateException("A decisão só pode ser registrada na etapa de aprovação");
        }
        if (order.getValue() <= 0) {
            throw new IllegalStateException("Orçamento precisa ser maior que zero");
        }

        order.setApproved(request.approved());

        if (request.approved()) {
            order.setStage(OrderStage.AGUARDANDO_INICIO);
            order.setReserved(true);
        } else {
            order.setStage(OrderStage.RECEPCAO_CHECKIN);
            order.setValue(0);
            order.setReserved(false);
        }

        order.setProgress(progressFor(order.getStage()));
        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder updateChecklist(UUID id, ChecklistRequest request) {
        if (request.checklist().size() != 5) {
            throw new IllegalArgumentException("Checklist deve conter exatamente 5 itens");
        }

        ServiceOrder order = findOrder(id);
        if (order.getStage() != OrderStage.CONTROLE_QUALIDADE) {
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
        ServiceOrder order = findOrder(id);

        if (order.getStage() != OrderStage.PRONTO_ENTREGA) {
            throw new IllegalStateException("Entrega bloqueada: OS não está pronta");
        }

        if (!COMPLETE_CHECKLIST.equals(order.getChecklist())) {
            throw new IllegalStateException("Entrega bloqueada: checklist pendente");
        }

        order.setReserved(false);
        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder addPhoto(UUID id) {
        ServiceOrder order = findOrder(id);
        order.setPhotos(order.getPhotos() + 1);
        return orderRepository.save(order);
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
