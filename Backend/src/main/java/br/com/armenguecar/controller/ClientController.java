package br.com.armenguecar.controller;

import br.com.armenguecar.dto.ClientDtos.CreateClientRequest;
import br.com.armenguecar.entity.Client;
import br.com.armenguecar.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<Client> list() {
        return clientService.list();
    }

    @PostMapping
    public Client create(@Valid @RequestBody CreateClientRequest request) {
        return clientService.create(request);
    }
}
