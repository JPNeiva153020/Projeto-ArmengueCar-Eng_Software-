package br.com.armenguecar.controller;

import br.com.armenguecar.entity.Vehicle;
import br.com.armenguecar.repository.ClientRepository;
import br.com.armenguecar.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("hasAnyRole('GERENTE','MECANICO','ADMIN')")
public class VehicleController {
    private final VehicleRepository vehicles;
    private final ClientRepository clients;
    private final br.com.armenguecar.repository.ServiceOrderRepository orders;
    public VehicleController(VehicleRepository vehicles, ClientRepository clients, br.com.armenguecar.repository.ServiceOrderRepository orders) {
        this.vehicles = vehicles; this.clients = clients; this.orders = orders;
    }
    public record VehicleRequest(@NotNull UUID clientId, @NotBlank @Size(max=255) String plate,
                                 @NotBlank @Size(max=255) String model, @Size(max=255) String brand,
                                 @Min(1886) @Max(2100) Integer modelYear) {}
    @GetMapping @Transactional(readOnly = true)
    public List<Vehicle> list() { return vehicles.findAll(); }
    @PostMapping @Transactional
    public Vehicle create(@Valid @RequestBody VehicleRequest request) {
        return save(new Vehicle(), request);
    }
    @PutMapping("/{id}") @Transactional
    public Vehicle update(@PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
        Vehicle v = vehicles.findByIdForUpdate(id).orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado"));
        return save(v, request);
    }
    private Vehicle save(Vehicle vehicle, VehicleRequest request) {
        if (vehicle.getId() != null && !vehicle.getClient().getId().equals(request.clientId())
                && orders.existsByVehicleRefId(vehicle.getId()))
            throw new IllegalArgumentException("Veículo com histórico de ordens não pode mudar de cliente");
        String plate = request.plate().trim().toUpperCase(Locale.ROOT);
        vehicles.findByPlateIgnoreCase(plate).filter(found -> !found.getId().equals(vehicle.getId()))
                .ifPresent(found -> { throw new IllegalArgumentException("Placa já cadastrada"); });
        vehicle.setClient(clients.findById(request.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado")));
        vehicle.setPlate(plate);
        vehicle.setModel(request.model().trim());
        vehicle.setBrand(request.brand() == null || request.brand().isBlank() ? null : request.brand().trim());
        vehicle.setModelYear(request.modelYear());
        return vehicles.save(vehicle);
    }
}
