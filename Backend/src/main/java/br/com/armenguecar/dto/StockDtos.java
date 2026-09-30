package br.com.armenguecar.dto;

import br.com.armenguecar.enums.MovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class StockDtos {

    private StockDtos() {
    }

    public record MovementRequest(
            @NotBlank String itemName,
            @NotBlank String unit,
            @Positive double quantity,
            @NotNull MovementType type,
            String reason
    ) {
    }

    public record UpdateStockRequest(
            @NotBlank String name,
            @NotBlank String code,
            @PositiveOrZero double quantity,
            @PositiveOrZero double reorderPoint,
            @NotBlank String unit
    ) {
    }
}
