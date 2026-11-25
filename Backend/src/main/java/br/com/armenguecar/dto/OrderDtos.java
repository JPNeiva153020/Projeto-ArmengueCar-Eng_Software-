package br.com.armenguecar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotNull java.util.UUID vehicleId,
            @NotBlank String problem
    ) {
    }

    public record UpdateBudgetRequest(
            @Positive double value
    ) {
    }

    public record ChecklistRequest(
            @NotNull List<@NotNull Boolean> checklist
    ) {
    }

    public record DecisionRequest(
            boolean approved
    ) {
    }
}
