package br.com.armenguecar.service;

import br.com.armenguecar.dto.StockDtos.MovementRequest;
import br.com.armenguecar.dto.StockDtos.UpdateStockRequest;
import br.com.armenguecar.entity.StockItem;
import br.com.armenguecar.entity.StockMovement;
import br.com.armenguecar.enums.MovementType;
import br.com.armenguecar.repository.StockItemRepository;
import br.com.armenguecar.repository.StockMovementRepository;
import br.com.armenguecar.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StockService {

    private final StockItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;

    public StockService(
            StockItemRepository itemRepository,
            StockMovementRepository movementRepository,
            UserRepository userRepository
    ) {
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StockItem> list() {
        return itemRepository.findAll();
    }

    @Transactional
    public StockMovement createMovement(MovementRequest request, UUID userId) {
        StockItem item = findOrCreateItem(request);
        validateMovement(item, request);

        applyMovement(item, request);
        itemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setType(request.type());
        movement.setQuantity(request.quantity());
        movement.setReason(request.reason());
        movement.setCreatedBy(userRepository.findById(userId).orElse(null));

        return movementRepository.save(movement);
    }

    @Transactional
    public StockItem update(UUID id, UpdateStockRequest request) {
        StockItem item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado"));

        item.setName(request.name().trim());
        item.setCode(request.code().trim().toUpperCase());
        item.setQuantity(request.quantity());
        item.setReorderPoint(request.reorderPoint());
        item.setUnit(request.unit().trim());

        return itemRepository.save(item);
    }

    private StockItem findOrCreateItem(MovementRequest request) {
        return itemRepository.findByNameIgnoreCase(request.itemName().trim())
                .orElseGet(() -> createNewItem(request));
    }

    private StockItem createNewItem(MovementRequest request) {
        if (request.type() == MovementType.SAIDA) {
            throw new EntityNotFoundException("Item não encontrado");
        }

        StockItem item = new StockItem();
        item.setName(request.itemName().trim());
        item.setCode("NOVO-" + System.currentTimeMillis());
        item.setQuantity(0);
        item.setReorderPoint(5);
        item.setUnit(request.unit().trim());
        return item;
    }

    private void validateMovement(StockItem item, MovementRequest request) {
        if (request.type() == MovementType.SAIDA && item.getQuantity() < request.quantity()) {
            throw new IllegalStateException("Saldo insuficiente para realizar a saída");
        }
    }

    private void applyMovement(StockItem item, MovementRequest request) {
        double quantity = request.quantity();
        double updatedQuantity = request.type() == MovementType.ENTRADA
                ? item.getQuantity() + quantity
                : item.getQuantity() - quantity;

        item.setQuantity(updatedQuantity);
    }
}
