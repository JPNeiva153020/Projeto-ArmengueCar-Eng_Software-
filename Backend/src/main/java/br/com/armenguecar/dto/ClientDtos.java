package br.com.armenguecar.dto;

import jakarta.validation.constraints.NotBlank;

public final class ClientDtos {

    private ClientDtos() {
    }

    public record CreateClientRequest(
            @NotBlank String name,
            @NotBlank String phone,
            String email,
            String plate,
            String vehicle
    ) {
    }
}
