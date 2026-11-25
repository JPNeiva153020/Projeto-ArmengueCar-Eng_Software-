package br.com.armenguecar.service;

import br.com.armenguecar.dto.ClientDtos.CreateClientRequest;
import br.com.armenguecar.entity.Client;
import br.com.armenguecar.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final br.com.armenguecar.repository.UserRepository users;
    private final AuditService audit;
    private final br.com.armenguecar.repository.VehicleRepository vehicles;

    public ClientService(ClientRepository clientRepository, br.com.armenguecar.repository.UserRepository users, AuditService audit, br.com.armenguecar.repository.VehicleRepository vehicles) {
        this.clientRepository = clientRepository;
        this.users = users;
        this.audit = audit;
        this.vehicles = vehicles;
    }

    @Transactional(readOnly = true)
    public List<Client> list() {
        return clientRepository.findAll();
    }

    @Transactional
    public Client create(CreateClientRequest request) {
        Client client = new Client();
        client.setName(request.name().trim());
        client.setPhone(request.phone().trim());
        client.setEmail(normalizeNullable(request.email()));
        client.setPlate(normalizePlate(request.plate()));
        client.setVehicle(normalizeNullable(request.vehicle()));

        if ((client.getPlate() == null) != (client.getVehicle() == null))
            throw new IllegalArgumentException("Informe placa e veículo juntos ou deixe ambos vazios");
        if (client.getPlate() != null && vehicles.findByPlateIgnoreCase(client.getPlate()).isPresent())
            throw new IllegalArgumentException("Placa já cadastrada");
        Client saved = clientRepository.save(client);
        if (saved.getPlate() != null) {
            var vehicle = new br.com.armenguecar.entity.Vehicle();
            vehicle.setClient(saved);
            vehicle.setPlate(saved.getPlate());
            vehicle.setModel(saved.getVehicle());
            vehicles.save(vehicle);
        }
        return saved;
    }

    @Transactional
    public Client linkUser(java.util.UUID clientId, java.util.UUID userId, java.util.UUID actorId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cliente não encontrado"));
        if (userId != null) {
            var user = users.findById(userId)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Usuário não encontrado"));
            if (user.getRole() != br.com.armenguecar.enums.Role.CLIENTE)
                throw new IllegalArgumentException("A conta vinculada deve ter perfil CLIENTE");
            clientRepository.findByUserId(userId).filter(other -> !other.getId().equals(clientId)).ifPresent(other -> {
                throw new IllegalArgumentException("A conta já está vinculada a outro cliente");
            });
        }
        client.setUserId(userId);
        audit.log(actorId, "CLIENTE_VINCULO_ALTERADO", clientId.toString(),
                userId == null ? "Vínculo removido" : "Conta vinculada: " + userId);
        return clientRepository.save(client);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizePlate(String plate) {
        return plate == null || plate.isBlank() ? null : plate.trim().toUpperCase();
    }
}
