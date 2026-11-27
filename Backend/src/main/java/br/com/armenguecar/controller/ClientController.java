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
    private final br.com.armenguecar.service.ClientRegistrationService registration;

    public ClientController(ClientService clientService, br.com.armenguecar.service.ClientRegistrationService registration) {
        this.clientService = clientService; this.registration=registration;
    }

    public record LinkUserRequest(java.util.UUID userId) {}
    public record ClientWithAccountRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String name,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String phone,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max=255) String email,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=8,max=72) String password,
        @jakarta.validation.constraints.Size(max=255) String plate,
        @jakarta.validation.constraints.Size(max=255) String vehicle) {}
    @PostMapping("/with-account")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Client createWithAccount(@Valid @RequestBody ClientWithAccountRequest r, org.springframework.security.core.Authentication auth) {
        var actor=(br.com.armenguecar.entity.User)auth.getPrincipal();
        return registration.create(r.name(),r.phone(),r.email(),r.password(),r.plate(),r.vehicle(),actor.getId());
    }


    @org.springframework.web.bind.annotation.PatchMapping("/{id}/user")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Client linkUser(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id,
                           @RequestBody LinkUserRequest request,
                           org.springframework.security.core.Authentication authentication) {
        var actor = (br.com.armenguecar.entity.User) authentication.getPrincipal();
        return clientService.linkUser(id, request.userId(), actor.getId());
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
