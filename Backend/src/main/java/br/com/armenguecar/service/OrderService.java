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

    public OrderService(
            ServiceOrderRepository orderRepository,
            ClientRepository clientRepository
    ) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
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
        Client client = clientRepository.findById(UUID.fromString(request.clientId()))
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        String plate = normalizePlate(request.plate());
        validateNoActiveOrder(plate);

        ServiceOrder order = new ServiceOrder();
        order.setNumber(generateNextNumber());
        order.setClient(client);
        order.setPlate(plate);
        order.setVehicle(request.vehicle().trim());
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
        order.setValue(request.value());
        return orderRepository.save(order);
    }

    @Transactional
    public ServiceOrder decide(UUID id, DecisionRequest request) {
        ServiceOrder order = findOrder(id);

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

    private void validateNoActiveOrder(String plate) {
        boolean hasActiveOrder = orderRepository.findAll().stream()
                .anyMatch(order -> plate.equalsIgnoreCase(order.getPlate())
                        && order.getStage() != OrderStage.PRONTO_ENTREGA);

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
