package com.thiago.financeapi.application.dto;

import com.thiago.financeapi.domain.model.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record AccountRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 60) String institution,
            @NotNull BigDecimal initialBalance) {
    }

    public record AccountResponse(
            UUID id,
            String name,
            String institution,
            BigDecimal initialBalance,
            BigDecimal currentBalance) {

        public static AccountResponse from(Account account, BigDecimal currentBalance) {
            return new AccountResponse(
                    account.getId(),
                    account.getName(),
                    account.getInstitution(),
                    account.getInitialBalance(),
                    currentBalance);
        }
    }
}
