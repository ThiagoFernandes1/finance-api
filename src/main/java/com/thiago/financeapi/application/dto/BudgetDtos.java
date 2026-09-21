package com.thiago.financeapi.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    public record BudgetRequest(
            @NotNull UUID categoryId,
            @NotNull @DecimalMin(value = "0.01", message = "Limite deve ser maior que zero")
            BigDecimal limitAmount,
            @NotNull YearMonth referenceMonth) {
    }

    /**
     * Resposta enriquecida: alem do limite, ja devolve o quanto foi consumido e a
     * situacao, para que o cliente nao precise cruzar dados de outro endpoint.
     */
    public record BudgetResponse(
            UUID id,
            UUID categoryId,
            String categoryName,
            YearMonth referenceMonth,
            BigDecimal limitAmount,
            BigDecimal spentAmount,
            BigDecimal remainingAmount,
            BigDecimal usagePercentage,
            BudgetStatus status) {
    }

    public enum BudgetStatus {
        /** Consumo ate 80% do limite. */
        ON_TRACK,
        /** Entre 80% e 100% do limite. */
        WARNING,
        /** Limite ultrapassado. */
        EXCEEDED
    }
}
