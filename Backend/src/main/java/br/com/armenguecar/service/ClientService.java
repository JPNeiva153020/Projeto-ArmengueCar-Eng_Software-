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

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
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

        return clientRepository.save(client);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizePlate(String plate) {
        return plate == null || plate.isBlank() ? null : plate.trim().toUpperCase();
    }
}
