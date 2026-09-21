package com.thiago.financeapi.application.dto;

import com.thiago.financeapi.domain.model.Transaction;
import com.thiago.financeapi.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransactionRequest(
            @NotBlank @Size(max = 160) String description,
            @NotNull @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
            BigDecimal amount,
            @NotNull TransactionType type,
            @NotNull LocalDate occurredOn,
            @NotNull UUID accountId,
            @NotNull UUID categoryId) {
    }

    public record TransactionResponse(
            UUID id,
            String description,
            BigDecimal amount,
            TransactionType type,
            LocalDate occurredOn,
            UUID accountId,
            String accountName,
            UUID categoryId,
            String categoryName) {

        public static TransactionResponse from(Transaction transaction) {
            return new TransactionResponse(
                    transaction.getId(),
                    transaction.getDescription(),
                    transaction.getAmount(),
                    transaction.getType(),
                    transaction.getOccurredOn(),
                    transaction.getAccount().getId(),
                    transaction.getAccount().getName(),
                    transaction.getCategory().getId(),
                    transaction.getCategory().getName());
        }
    }

    /** Filtros opcionais do extrato; qualquer campo nulo e ignorado na query. */
    public record TransactionFilter(
            LocalDate startDate,
            LocalDate endDate,
            UUID accountId,
            UUID categoryId,
            TransactionType type,
            String description) {
    }
}
